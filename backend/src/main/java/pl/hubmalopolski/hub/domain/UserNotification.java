package pl.hubmalopolski.hub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_notification")
public class UserNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private AppUser recipient;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idea_id")
    private Idea idea;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationKind kind;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private NotificationTargetType targetType;
    @Column(name = "target_id", nullable = false)
    private Long targetId;
    @Column(nullable = false)
    private String title;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "read_at")
    private Instant readAt;

    protected UserNotification() {}

    public UserNotification(AppUser recipient, Idea idea, NotificationKind kind, String title) {
        this.recipient = recipient;
        this.idea = idea;
        this.kind = kind;
        this.targetType = NotificationTargetType.IDEA;
        this.targetId = idea.getId();
        this.title = title;
    }

    public UserNotification(AppUser recipient, NotificationKind kind, NotificationTargetType targetType,
                            Long targetId, String title) {
        this.recipient = recipient;
        this.kind = kind;
        this.targetType = targetType;
        this.targetId = targetId;
        this.title = title;
    }

    public Long getId() { return id; }
    public AppUser getRecipient() { return recipient; }
    public Idea getIdea() { return idea; }
    public Long getIdeaId() { return idea == null ? null : idea.getId(); }
    public NotificationKind getKind() { return kind; }
    public NotificationTargetType getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getTitle() { return title; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public void markRead() { if (readAt == null) readAt = Instant.now(); }
}
