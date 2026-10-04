package pl.hubmalopolski.hub.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "problem_report")
public class ProblemReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, columnDefinition = "text")
    private String description;
    @Enumerated(EnumType.STRING)
    private Region region;
    @Column(name = "author_name")
    private String authorName;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_id")
    private ChallengeArea area;               // nadany przez AI lub admina
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.NOWE;
    @Column(name = "created_at", nullable = false)
    private final Instant createdAt = Instant.now();

    protected ProblemReport() {
    }

    public ProblemReport(String description, Region region, String authorName) {
        this.description = description;
        this.region = region;
        this.authorName = authorName;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Region getRegion() {
        return region;
    }

    public String getAuthorName() {
        return authorName;
    }

    public ChallengeArea getArea() {
        return area;
    }

    public void setArea(ChallengeArea area) {
        this.area = area;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
