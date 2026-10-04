package pl.hubmalopolski.hub.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.hubmalopolski.hub.domain.ProblemReport;

import java.time.Instant;
import java.util.List;

public interface ProblemReportRepository extends JpaRepository<ProblemReport, Long> {
    List<ProblemReport> findAllByOrderByCreatedAtDesc();

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);

    @Query("select a.id, a.name, count(r) from ProblemReport r left join r.area a " +
            "where r.createdAt >= :from and r.createdAt < :to " +
            "group by a.id, a.name order by count(r) desc")
    List<Object[]> countByAreaInPeriod(Instant from, Instant to);

    @Query("select r.region, count(r) from ProblemReport r " +
            "where r.createdAt >= :from and r.createdAt < :to " +
            "group by r.region order by count(r) desc")
    List<Object[]> countByRegionInPeriod(Instant from, Instant to);

    @Query("select r.status, count(r) from ProblemReport r " +
            "where r.createdAt >= :from and r.createdAt < :to " +
            "group by r.status order by count(r) desc")
    List<Object[]> countByStatusInPeriod(Instant from, Instant to);

    @Query("select year(r.createdAt), month(r.createdAt), count(r) from ProblemReport r " +
            "where r.createdAt >= :from and r.createdAt < :to " +
            "group by year(r.createdAt), month(r.createdAt) " +
            "order by year(r.createdAt), month(r.createdAt)")
    List<Object[]> countByMonthInPeriod(Instant from, Instant to);
}
