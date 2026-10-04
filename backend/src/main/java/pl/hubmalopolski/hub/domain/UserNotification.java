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
    @JoinColumn(name = "idea_id", nullable = false)
    private Idea idea;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationKind kind;
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
        this.title = title;
    }

    public Long getId() { return id; }
    public AppUser getRecipient() { return recipient; }
    public Idea getIdea() { return idea; }
    public NotificationKind getKind() { return kind; }
    public String getTitle() { return title; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public void markRead() { if (readAt == null) readAt = Instant.now(); }
}
