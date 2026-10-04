package pl.hubmalopolski.hub.grants;

import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.hubmalopolski.hub.domain.AppUser;
import pl.hubmalopolski.hub.domain.AppUserRole;
import pl.hubmalopolski.hub.domain.HubWorkflowRecord;
import pl.hubmalopolski.hub.domain.NotificationKind;
import pl.hubmalopolski.hub.domain.NotificationTargetType;
import pl.hubmalopolski.hub.domain.UserNotification;
import pl.hubmalopolski.hub.repo.AppUserRepository;
import pl.hubmalopolski.hub.repo.HubWorkflowRecordRepository;
import pl.hubmalopolski.hub.repo.UserNotificationRepository;
import pl.hubmalopolski.hub.workflow.HubWorkflowDto;
import pl.hubmalopolski.hub.workflow.WorkflowSupport;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class GrantService {
    private static final String CALL = "GRANT_CALL";
    private static final String APPLICATION = "GRANT_APPLICATION";
    private static final Set<String> CALL_STATUSES = Set.of("DRAFT", "OPEN", "CLOSED");
    private static final Set<String> APPLICATION_STATUSES = Set.of("SUBMITTED", "UNDER_REVIEW", "APPROVED", "REJECTED");
    private static final Set<String> FIELD_TYPES = Set.of("TEXT", "TEXTAREA", "NUMBER", "SELECT", "CHECKBOX");
    private static final Pattern FIELD_KEY = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]{0,39}");

    public record Field(String key, String label, String type, boolean required, List<String> options) {}

    private final HubWorkflowRecordRepository workflows;
    private final UserNotificationRepository notifications;
    private final AppUserRepository users;
    private final WorkflowSupport support;

    public GrantService(HubWorkflowRecordRepository workflows, UserNotificationRepository notifications,
                        AppUserRepository users, WorkflowSupport support) {
        this.workflows = workflows;
        this.notifications = notifications;
        this.users = users;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public List<HubWorkflowDto> openCalls() {
        Instant now = Instant.now();
        return workflows.findByModuleAndStatusOrderByCreatedAtDescIdDesc(CALL, "OPEN").stream()
                .filter(call -> isOpenAt(call, now)).map(call -> support.dto(call, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<HubWorkflowDto> adminCalls(String email) {
        support.requireAdmin(email);
        return workflows.findByModuleOrderByCreatedAtDescIdDesc(CALL).stream()
                .map(call -> support.dto(call, false)).toList();
    }

    @Transactional
    public HubWorkflowDto createCall(String email, String title, String description, Instant opensAt,
                                     Instant closesAt, String status, List<Field> fields) {
        support.requireAdmin(email);
        AppUser admin = support.requireUser(email);
        validateCall(title, description, opensAt, closesAt, status, fields);
        String actualStatus = status == null ? "DRAFT" : status;
        Map<String, Object> payload = callPayload(description, opensAt, closesAt, fields);
        HubWorkflowRecord saved = workflows.save(new HubWorkflowRecord(CALL, admin, null, actualStatus,
                title.trim(), support.writePayload(payload)));
        return support.dto(saved, false);
    }

    @Transactional
    public HubWorkflowDto updateCall(long id, String email, String title, String description, Instant opensAt,
                                     Instant closesAt, String status, List<Field> fields) {
        support.requireAdmin(email);
        validateCall(title, description, opensAt, closesAt, status, fields);
        HubWorkflowRecord call = workflows.findByIdAndModule(id, CALL)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String actualStatus = status == null ? call.getStatus() : status;
        call.replace(actualStatus, title.trim(), support.writePayload(callPayload(description, opensAt, closesAt, fields)));
        return support.dto(workflows.save(call), false);
    }

    @Transactional
    public HubWorkflowDto submitApplication(long callId, String email, Map<String, String> answers) {
        AppUser member = support.requireMember(email);
        HubWorkflowRecord call = workflows.findByIdAndModule(callId, CALL)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!"OPEN".equals(call.getStatus()) || !isOpenAt(call, Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nabór nie jest otwarty");
        }
        if (workflows.findByModuleAndReferenceAndOwner(APPLICATION, callId, member.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wniosek do tego naboru został już złożony");
        }
        Map<String, Object> callData = support.readPayload(call.getPayload());
        List<Field> fields = readFields(callData.get("fields"));
        validateAnswers(fields, answers);
        if (answers.values().stream().allMatch(value -> value == null || value.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wypełnij przynajmniej jedno pole wniosku");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("answers", new LinkedHashMap<>(answers));
        payload.put("formSnapshot", callData);
        final HubWorkflowRecord saved;
        try {
            saved = workflows.save(new HubWorkflowRecord(APPLICATION, member, callId,
                    "SUBMITTED", call.getTitle(), support.writePayload(payload),
                    "application:" + member.getId() + ":" + callId));
        } catch (DataIntegrityViolationException duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wniosek do tego naboru został już złożony");
        }
        String title = "Nowy wniosek: " + call.getTitle();
        if (title.length() > 255) title = title.substring(0, 255);
        for (AppUser recipient : users.findByRoleIn(List.of(AppUserRole.STAFF, AppUserRole.ADMIN))) {
            notifications.save(new UserNotification(recipient, NotificationKind.NEW_GRANT_APPLICATION,
                    NotificationTargetType.GRANT_APPLICATION, saved.getId(), title));
        }
        return support.dto(saved, false);
    }

    @Transactional(readOnly = true)
    public List<HubWorkflowDto> myApplications(String email) {
        support.requireMember(email);
        return workflows.findByModuleAndOwnerEmailOrderByCreatedAtDescIdDesc(APPLICATION, email).stream()
                .map(record -> support.dto(record, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<HubWorkflowDto> staffApplications(String email) {
        support.requireStaff(email);
        return workflows.findByModuleOrderByCreatedAtDescIdDesc(APPLICATION).stream()
                .map(record -> support.dto(record, true)).toList();
    }

    @Transactional
    public HubWorkflowDto changeApplicationStatus(long id, String email, String status) {
        support.requireStaff(email);
        if (!APPLICATION_STATUSES.contains(status) || "SUBMITTED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieprawidłowy status wniosku");
        }
        HubWorkflowRecord application = workflows.findByIdAndModule(id, APPLICATION)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        application.replace(status, application.getTitle(), application.getPayload());
        HubWorkflowRecord saved = workflows.save(application);
        String title = "Zmiana statusu wniosku: " + saved.getTitle();
        if (title.length() > 255) title = title.substring(0, 255);
        notifications.save(new UserNotification(saved.getOwner(), NotificationKind.GRANT_STATUS,
                NotificationTargetType.GRANT_APPLICATION, saved.getId(), title));
        return support.dto(saved, true);
    }

    private boolean isOpenAt(HubWorkflowRecord call, Instant now) {
        Map<String, Object> data = support.readPayload(call.getPayload());
        try {
            Instant opensAt = Instant.parse(String.valueOf(data.get("opensAt")));
            Instant closesAt = Instant.parse(String.valueOf(data.get("closesAt")));
            return !now.isBefore(opensAt) && now.isBefore(closesAt);
        } catch (RuntimeException e) { return false; }
    }

    private static void validateCall(String title, String description, Instant opensAt, Instant closesAt,
                                     String status, List<Field> fields) {
        if (title == null || title.isBlank() || title.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tytuł naboru jest wymagany (maks. 255 znaków)");
        }
        if (description != null && description.length() > 4000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if (opensAt == null || closesAt == null || !opensAt.isBefore(closesAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Okno naboru jest nieprawidłowe");
        }
        if (status != null && !CALL_STATUSES.contains(status)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if (fields == null || fields.isEmpty() || fields.size() > 40) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nabór musi zawierać od 1 do 40 pól");
        }
        Set<String> keys = new java.util.HashSet<>();
        for (Field field : fields) {
            if (field == null || field.key() == null || !FIELD_KEY.matcher(field.key()).matches()
                    || field.label() == null || field.label().isBlank() || field.label().length() > 160
                    || field.type() == null || !FIELD_TYPES.contains(field.type()) || !keys.add(field.key())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Definicja formularza naboru jest nieprawidłowa");
            }
            List<String> options = field.options() == null ? List.of() : field.options();
            if ("SELECT".equals(field.type()) && (options.isEmpty() || options.size() > 50
                    || options.stream().anyMatch(option -> option == null || option.isBlank() || option.length() > 120)
                    || options.stream().distinct().count() != options.size())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pola SELECT wymagają unikalnych opcji");
            }
            if (!"SELECT".equals(field.type()) && !options.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opcje są dozwolone wyłącznie dla SELECT");
            }
        }
    }

    private Map<String, Object> callPayload(String description, Instant opensAt, Instant closesAt, List<Field> fields) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("description", description == null ? "" : description.trim());
        payload.put("opensAt", opensAt.toString());
        payload.put("closesAt", closesAt.toString());
        payload.put("fields", fields.stream().map(field -> Map.of("key", field.key(), "label", field.label().trim(),
                "type", field.type(), "required", field.required(), "options",
                field.options() == null ? List.of() : field.options())).toList());
        return payload;
    }

    private static List<Field> readFields(Object raw) {
        if (!(raw instanceof List<?> list)) throw new IllegalStateException("Grant form definition is invalid");
        List<Field> fields = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) throw new IllegalStateException("Grant form field is invalid");
            Object rawOptions = map.get("options");
            List<String> options = rawOptions instanceof List<?> values
                    ? values.stream().map(String::valueOf).toList() : List.of();
            fields.add(new Field(String.valueOf(map.get("key")), String.valueOf(map.get("label")),
                    String.valueOf(map.get("type")), Boolean.TRUE.equals(map.get("required")), options));
        }
        return fields;
    }

    private static void validateAnswers(List<Field> fields, Map<String, String> answers) {
        if (answers == null || answers.size() > 40) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        Map<String, Field> byKey = fields.stream().collect(java.util.stream.Collectors.toMap(Field::key, field -> field));
        if (!byKey.keySet().containsAll(answers.keySet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Odpowiedzi zawierają nieznane pola");
        }
        for (Field field : fields) {
            String value = answers.get(field.key());
            if (field.required() && (value == null || value.isBlank())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uzupełnij pole: " + field.label());
            }
            if (value == null) continue;
            if (value.length() > 10000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            if ("SELECT".equals(field.type()) && !field.options().contains(value)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wybierz jedną z dostępnych opcji");
            }
            if ("CHECKBOX".equals(field.type()) && !Set.of("true", "false").contains(value)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieprawidłowa odpowiedź TAK/NIE");
            }
            if ("NUMBER".equals(field.type())) {
                try { new BigDecimal(value); }
                catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wpisz liczbę"); }
            }
        }
    }
}
