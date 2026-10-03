package pl.hubmalopolski.hub.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Fiszka pomyslu na innowacje spoleczna (modul III).
 */
@Entity
@Table(name = "idea")
public class Idea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "text")
    private String essence;        // na czym polega
    @Column(name = "target_group")
    private String targetGroup;    // komu sluzy
    @Enumerated(EnumType.STRING)
    private IdeaStage stage = IdeaStage.MYSL;
    @Column(columnDefinition = "text")
    private String description;
    @Column(nullable = false)
    private String author = "Anonim";
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_user_id")
    private AppUser owner;
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    private IdeaModerationStatus moderationStatus = IdeaModerationStatus.PENDING;
    @Column(name = "created_at", nullable = false)
    private final Instant createdAt = Instant.now();

    protected Idea() {
    }

    public Idea(String title, String essence, String targetGroup, IdeaStage stage, String description,
                String author, AppUser owner) {
        this.title = title;
        this.essence = essence;
        this.targetGroup = targetGroup;
        this.stage = stage == null ? IdeaStage.MYSL : stage;
        this.description = description;
        this.author = author;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getEssence() {
        return essence;
    }

    public String getTargetGroup() {
        return targetGroup;
    }

    public IdeaStage getStage() {
        return stage;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() { return author; }

    public AppUser getOwner() { return owner; }

    public IdeaModerationStatus getModerationStatus() { return moderationStatus; }

    public void setModerationStatus(IdeaModerationStatus moderationStatus) {
        this.moderationStatus = moderationStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
