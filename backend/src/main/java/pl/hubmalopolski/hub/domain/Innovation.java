package pl.hubmalopolski.hub.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "innovation")
public class Innovation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    private String summary;
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "target_group")
    private String targetGroup;
    private String status;          // TESTOWANA | WDROZONA | ROZWOJ
    private String region;
    @Column(name = "video_url")
    private String videoUrl;
    @Column(name = "source_url")
    private String sourceUrl;      // real, clickable link to the innovation (ROPS library / official site)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_id")
    private ChallengeArea area;
    @Column(name = "vector_id")
    private String vectorId;        // dokument w vector_store (Spring AI pgvector)

    protected Innovation() {
    }

    public Innovation(String title, String summary, String description,
                      String targetGroup, String status, String region, String videoUrl) {
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

    public String getStatus() {
        return status;
    }

    public String getRegion() {
        return region;
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
