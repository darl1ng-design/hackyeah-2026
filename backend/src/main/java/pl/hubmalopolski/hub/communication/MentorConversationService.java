package pl.hubmalopolski.hub.communication;

import org.springframework.http.HttpStatus;
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
import pl.hubmalopolski.hub.workflow.WorkflowSupport;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MentorConversationService {
    private static final String CONVERSATION = "MENTOR_CONVERSATION";
    private static final String MESSAGE = "MENTOR_MESSAGE";
    public record MessageDto(Long id, String author, String authorRole, String body, Instant createdAt) {}
    public record ConversationDto(Long id, String subject, String status, String author,
                                  Instant createdAt, List<MessageDto> messages) {}

    private final HubWorkflowRecordRepository workflows;
    private final UserNotificationRepository notifications;
    private final AppUserRepository users;
    private final WorkflowSupport support;

    public MentorConversationService(HubWorkflowRecordRepository workflows, UserNotificationRepository notifications,
            AppUserRepository users, WorkflowSupport support) {
        this.workflows = workflows;
        this.notifications = notifications;
        this.users = users;
        this.support = support;
    }

    @Transactional
    public ConversationDto open(String email, String subject, String body) {
        AppUser member = support.requireMember(email);
        String cleanSubject = requiredText(subject, 180, "Temat");
        String cleanBody = requiredText(body, 4000, "Wiadomość");
        HubWorkflowRecord thread = workflows.save(new HubWorkflowRecord(CONVERSATION, member, null,
                "OPEN", cleanSubject, "{}"));
        saveMessage(thread, member, cleanBody);
        notifyStaff(thread, "Nowa rozmowa: " + cleanSubject);
        return toDto(thread, true);
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> mine(String email) {
        support.requireMember(email);
        return workflows.findByModuleAndOwnerEmailOrderByCreatedAtDescIdDesc(CONVERSATION, email).stream()
                .map(thread -> toDto(thread, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> staffList(String email) {
        support.requireStaff(email);
        return workflows.findByModuleOrderByCreatedAtDescIdDesc(CONVERSATION).stream()
                .map(thread -> toDto(thread, false)).toList();
    }

    @Transactional(readOnly = true)
    public ConversationDto get(long id, String email) {
        AppUser viewer = support.requireUser(email);
        HubWorkflowRecord thread = requireConversation(id);
        boolean staff = viewer.getRole() == AppUserRole.STAFF || viewer.getRole() == AppUserRole.ADMIN;
        if (!staff && !thread.getOwner().getId().equals(viewer.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return toDto(thread, true);
    }

    @Transactional
    public ConversationDto message(long id, String email, String body) {
        AppUser member = support.requireMember(email);
        HubWorkflowRecord thread = requireConversation(id);
        if (!thread.getOwner().getId().equals(member.getId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!"OPEN".equals(thread.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Rozmowa jest zamknięta");
        saveMessage(thread, member, requiredText(body, 4000, "Wiadomość"));
        notifyStaff(thread, "Nowa wiadomość: " + thread.getTitle());
        return toDto(thread, true);
    }

    @Transactional
    public ConversationDto reply(long id, String email, String body) {
        support.requireStaff(email);
        AppUser staff = support.requireUser(email);
        HubWorkflowRecord thread = requireConversation(id);
        if (!"OPEN".equals(thread.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Rozmowa jest zamknięta");
        saveMessage(thread, staff, requiredText(body, 4000, "Wiadomość"));
        notifications.save(new UserNotification(thread.getOwner(), NotificationKind.MENTOR_MESSAGE,
                NotificationTargetType.MENTOR_CONVERSATION, thread.getId(), "Odpowiedź eksperta: " + thread.getTitle()));
        return toDto(thread, true);
    }

    private HubWorkflowRecord requireConversation(long id) {
        return workflows.findByIdAndModule(id, CONVERSATION)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void saveMessage(HubWorkflowRecord thread, AppUser author, String body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("body", body);
        workflows.save(new HubWorkflowRecord(MESSAGE, author, thread.getId(), "SENT", thread.getTitle(),
                support.writePayload(payload)));
    }

    private ConversationDto toDto(HubWorkflowRecord thread, boolean includeMessages) {
        List<MessageDto> messages = includeMessages
                ? workflows.findByModuleAndReference(MESSAGE, thread.getId()).stream()
                    .map(message -> new MessageDto(message.getId(), message.getOwner().getDisplayName(),
                            message.getOwner().getRole().name(),
                            String.valueOf(support.readPayload(message.getPayload()).get("body")),
                            message.getCreatedAt())).toList()
                : List.of();
        return new ConversationDto(thread.getId(), thread.getTitle(), thread.getStatus(),
                thread.getOwner().getDisplayName(), thread.getCreatedAt(), messages);
    }

    private void notifyStaff(HubWorkflowRecord thread, String title) {
        if (title.length() > 255) title = title.substring(0, 255);
        for (AppUser recipient : users.findByRoleIn(List.of(AppUserRole.STAFF, AppUserRole.ADMIN))) {
            notifications.save(new UserNotification(recipient, NotificationKind.MENTOR_MESSAGE,
                    NotificationTargetType.MENTOR_CONVERSATION, thread.getId(), title));
        }
    }

    private static String requiredText(String value, int max, String label) {
        if (value == null || value.isBlank() || value.trim().length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " jest wymagany (maks. " + max + " znaków)");
        }
        return value.trim();
    }
}
