package pl.hubmalopolski.hub.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.hubmalopolski.hub.domain.ReportMatch;

import java.util.List;

public interface ReportMatchRepository extends JpaRepository<ReportMatch, Long> {
    List<ReportMatch> findByReportIdOrderByRankOrderAsc(Long reportId);
}
