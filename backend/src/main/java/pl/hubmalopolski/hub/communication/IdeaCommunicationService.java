package pl.hubmalopolski.hub.communication;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.hubmalopolski.hub.domain.AppUser;
import pl.hubmalopolski.hub.domain.AppUserRole;
import pl.hubmalopolski.hub.domain.Idea;
import pl.hubmalopolski.hub.domain.IdeaReply;
import pl.hubmalopolski.hub.domain.NotificationKind;
import pl.hubmalopolski.hub.domain.UserNotification;
import pl.hubmalopolski.hub.repo.AppUserRepository;
import pl.hubmalopolski.hub.repo.IdeaReplyRepository;
import pl.hubmalopolski.hub.repo.IdeaRepository;
import pl.hubmalopolski.hub.repo.UserNotificationRepository;

import java.time.Instant;
import java.util.List;

@Service
public class IdeaCommunicationService {
    public record ReplyDto(Long id, Long ideaId, String body, String author, Instant createdAt) {}
    public record NotificationDto(Long id, Long ideaId, String kind, String title,
                                  Instant createdAt, boolean read) {}

    private final AppUserRepository users;
    private final IdeaRepository ideas;
    private final IdeaReplyRepository replies;
    private final UserNotificationRepository notifications;

    public IdeaCommunicationService(AppUserRepository users, IdeaRepository ideas,
                                    IdeaReplyRepository replies, UserNotificationRepository notifications) {
        this.users = users;
        this.ideas = ideas;
        this.replies = replies;
        this.notifications = notifications;
    }

    @Transactional
    public void onIdeaSubmitted(Idea idea) {
        for (AppUser recipient : users.findByRoleIn(List.of(AppUserRole.STAFF, AppUserRole.ADMIN))) {
            notifications.save(new UserNotification(recipient, idea, NotificationKind.NEW_IDEA,
                    notificationTitle("Nowy pomysł: ", idea.getTitle())));
        }
    }

    @Transactional
    public ReplyDto reply(long ideaId, String staffEmail, String body) {
        Idea idea = ideas.findById(ideaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (idea.getOwner() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Pomysł nie ma konta autora, do którego można wysłać odpowiedź");
        }
        AppUser staff = users.findByEmail(staffEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (staff.getRole() != AppUserRole.STAFF && staff.getRole() != AppUserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        IdeaReply saved = replies.save(new IdeaReply(idea, staff, body.trim()));
        notifications.save(new UserNotification(idea.getOwner(), idea, NotificationKind.IDEA_REPLY,
                notificationTitle("Odpowiedź na pomysł: ", idea.getTitle())));
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ReplyDto> replies(long ideaId, String email) {
        Idea idea = ideas.findById(ideaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        AppUser viewer = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        boolean staff = viewer.getRole() == AppUserRole.STAFF || viewer.getRole() == AppUserRole.ADMIN;
        if (!staff && (idea.getOwner() == null || !idea.getOwner().getId().equals(viewer.getId()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return replies.findByIdeaIdOrderByCreatedAtAscIdAsc(ideaId).stream()
                .map(IdeaCommunicationService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> notifications(String email) {
        return notifications.findByRecipientEmailOrderByCreatedAtDescIdDesc(email).stream()
                .map(IdeaCommunicationService::toDto).toList();
    }

    @Transactional
    public NotificationDto markRead(long id, String email) {
        UserNotification notification = notifications.findById(id)
                .filter(item -> item.getRecipient().getEmail().equals(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        notification.markRead();
        return toDto(notification);
    }

    private static ReplyDto toDto(IdeaReply reply) {
        return new ReplyDto(reply.getId(), reply.getIdea().getId(), reply.getBody(),
                reply.getAuthor().getDisplayName(), reply.getCreatedAt());
    }

    private static NotificationDto toDto(UserNotification notification) {
        return new NotificationDto(notification.getId(), notification.getIdea().getId(),
                notification.getKind().name(), notification.getTitle(), notification.getCreatedAt(),
                notification.getReadAt() != null);
    }

    private static String notificationTitle(String prefix, String ideaTitle) {
        String title = prefix + ideaTitle;
        return title.length() <= 255 ? title : title.substring(0, 255);
    }
}
