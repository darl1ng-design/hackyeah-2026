package pl.hubmalopolski.hub.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "innovation")
public class Innovation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(length = 1000)
    private String summary;
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "target_group")
    private String targetGroup;
    @Enumerated(EnumType.STRING)
    private InnovationStatus status;
    @Enumerated(EnumType.STRING)
    private Region region;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "video_url", length = 500)
    private String videoUrl;
    @Column(name = "source_url", length = 500)
    private String sourceUrl;      // real, clickable link to the innovation (ROPS library / official site)
    @Column(nullable = false)
    private boolean published = true;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_id")
    private ChallengeArea area;
    @Column(name = "vector_id")
    private String vectorId;        // dokument w vector_store (Spring AI pgvector)

    protected Innovation() {
    }

    public Innovation(String title, String summary, String description,
                      String targetGroup, InnovationStatus status, Region region, String videoUrl) {
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.targetGroup = targetGroup;
        this.status = status;
        this.region = region;
        this.videoUrl = videoUrl;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public String getTargetGroup() {
        return targetGroup;
    }

    public InnovationStatus getStatus() {
        return status;
    }

    public Region getRegion() {
        return region;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public void setTitle(String title) { this.title = title; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setDescription(String description) { this.description = description; }
    public void setTargetGroup(String targetGroup) { this.targetGroup = targetGroup; }
    public void setStatus(InnovationStatus status) { this.status = status; }
    public void setRegion(Region region) { this.region = region; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public ChallengeArea getArea() {
        return area;
    }

    public void setArea(ChallengeArea area) {
        this.area = area;
    }

    public String getVectorId() {
        return vectorId;
    }

    public void setVectorId(String vectorId) {
        this.vectorId = vectorId;
    }
}
