package pl.hubmalopolski.hub.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Fiszka pomyslu na innowacje spoleczna (modul III). */
@Entity
@Table(name = "idea")
public class Idea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "text")
    private String essence;        // na czym polega
    @Column(name = "target_group")
    private String targetGroup;    // komu sluzy
    private String stage;          // MYSL | PROTOTYP | TESTY | WDROZENIE
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Idea() {}
    public Idea(String title, String essence, String targetGroup, String stage, String description) {
        this.title = title; this.essence = essence; this.targetGroup = targetGroup;
        this.stage = stage; this.description = description;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getEssence() { return essence; }
    public String getTargetGroup() { return targetGroup; }
    public String getStage() { return stage; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}
