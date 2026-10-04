package pl.hubmalopolski.hub.testing;

import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.hubmalopolski.hub.domain.AppUser;
import pl.hubmalopolski.hub.domain.AppUserRole;
import pl.hubmalopolski.hub.domain.HubWorkflowRecord;
import pl.hubmalopolski.hub.domain.Innovation;
import pl.hubmalopolski.hub.domain.NotificationKind;
import pl.hubmalopolski.hub.domain.NotificationTargetType;
import pl.hubmalopolski.hub.domain.UserNotification;
import pl.hubmalopolski.hub.repo.AppUserRepository;
import pl.hubmalopolski.hub.repo.HubWorkflowRecordRepository;
import pl.hubmalopolski.hub.repo.InnovationRepository;
import pl.hubmalopolski.hub.repo.UserNotificationRepository;
import pl.hubmalopolski.hub.workflow.HubWorkflowDto;
import pl.hubmalopolski.hub.workflow.WorkflowSupport;

import java.util.LinkedHashMap;
import java.util.List;

@Service
public class InnovationTestingService {
    private static final String MODULE = "TESTER_FEEDBACK";
    private final InnovationRepository innovations;
    private final HubWorkflowRecordRepository workflows;
    private final UserNotificationRepository notifications;
    private final AppUserRepository users;
    private final WorkflowSupport support;

    public InnovationTestingService(InnovationRepository innovations, HubWorkflowRecordRepository workflows,
            UserNotificationRepository notifications, AppUserRepository users, WorkflowSupport support) {
        this.innovations = innovations;
        this.workflows = workflows;
        this.notifications = notifications;
        this.users = users;
        this.support = support;
    }

    @Transactional
    public HubWorkflowDto submit(long innovationId, String email, boolean interested, Integer rating,
                                 String feedback, String suggestion) {
        AppUser member = support.requireMember(email);
        Innovation innovation = innovations.findById(innovationId).filter(Innovation::isPublished)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!interested) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Udział w testach musi być potwierdzony");
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ocena musi mieścić się w zakresie 1–5");
        }
        var payload = new LinkedHashMap<String, Object>();
        payload.put("interested", true);
        payload.put("rating", rating);
        payload.put("feedback", trimOrNull(feedback));
        payload.put("suggestion", trimOrNull(suggestion));
        HubWorkflowRecord record = workflows.findByModuleAndReferenceAndOwner(MODULE, innovationId, member.getId())
                .orElseGet(() -> new HubWorkflowRecord(MODULE, member, innovationId, "SUBMITTED",
                        innovation.getTitle(), support.writePayload(payload), "tester:" + member.getId() + ":" + innovationId));
        record.replace("SUBMITTED", innovation.getTitle(), support.writePayload(payload));
        final HubWorkflowRecord saved;
        try { saved = workflows.save(record); }
        catch (DataIntegrityViolationException duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Zgłoszenie testera dla tej innowacji już istnieje");
        }
        String title = "Zgłoszenie testera: " + innovation.getTitle();
        if (title.length() > 255) title = title.substring(0, 255);
        for (AppUser recipient : users.findByRoleIn(List.of(AppUserRole.STAFF, AppUserRole.ADMIN))) {
            notifications.save(new UserNotification(recipient, NotificationKind.TESTER_ACTIVITY,
                    NotificationTargetType.TESTER_FEEDBACK, saved.getId(), title));
        }
        return support.dto(saved, false);
    }

    @Transactional(readOnly = true)
    public HubWorkflowDto mine(long innovationId, String email) {
        AppUser member = support.requireMember(email);
        return workflows.findByModuleAndReferenceAndOwner(MODULE, innovationId, member.getId())
                .map(record -> support.dto(record, false))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<HubWorkflowDto> staffList(String email) {
        support.requireStaff(email);
        return workflows.findByModuleOrderByCreatedAtDescIdDesc(MODULE).stream()
                .map(record -> support.dto(record, true)).toList();
    }

    private static String trimOrNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
