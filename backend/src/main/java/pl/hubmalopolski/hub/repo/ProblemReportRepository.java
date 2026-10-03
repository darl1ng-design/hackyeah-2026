package pl.hubmalopolski.hub.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.hubmalopolski.hub.domain.ProblemReport;

import java.util.List;

public interface ProblemReportRepository extends JpaRepository<ProblemReport, Long> {
    List<ProblemReport> findAllByOrderByCreatedAtDesc();
}
