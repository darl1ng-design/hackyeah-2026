package pl.hubmalopolski.hub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "report_match")
public class ReportMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private ProblemReport report;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "innovation_id", nullable = false)
    private Innovation innovation;
    @Column(name = "rank_order", nullable = false)
    private int rankOrder;
    @Column(nullable = false, columnDefinition = "text")
    private String why;
    @Column(nullable = false)
    private double similarity;

    protected ReportMatch() {}

    public ReportMatch(ProblemReport report, Innovation innovation, int rankOrder,
                       String why, double similarity) {
        this.report = report;
        this.innovation = innovation;
        this.rankOrder = rankOrder;
        this.why = why;
        this.similarity = similarity;
    }

    public Long getId() { return id; }
    public ProblemReport getReport() { return report; }
    public Innovation getInnovation() { return innovation; }
    public int getRankOrder() { return rankOrder; }
    public String getWhy() { return why; }
    public double getSimilarity() { return similarity; }
}
