package pl.hubmalopolski.hub.trends;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.hubmalopolski.hub.domain.Region;
import pl.hubmalopolski.hub.domain.ReportStatus;
import pl.hubmalopolski.hub.repo.ProblemReportRepository;

import java.time.Instant;
import java.util.List;

@Service
public class TrendService {
    public record CountDto(String key, String label, long count) {}
    public record MonthCountDto(String month, long count) {}
    public record TrendsDto(long total, List<CountDto> byArea, List<CountDto> byRegion,
                            List<CountDto> byStatus, List<MonthCountDto> byMonth) {}

    private final ProblemReportRepository reports;

    public TrendService(ProblemReportRepository reports) {
        this.reports = reports;
    }

    @Transactional(readOnly = true)
    public TrendsDto get(Instant from, Instant to) {
        List<CountDto> byArea = reports.countByAreaInPeriod(from, to).stream()
                .map(row -> new CountDto(row[0] == null ? "UNASSIGNED" : row[0].toString(),
                        row[1] == null ? "Nieprzypisany" : row[1].toString(), ((Number) row[2]).longValue()))
                .toList();
        List<CountDto> byRegion = reports.countByRegionInPeriod(from, to).stream()
                .map(row -> {
                    Region region = (Region) row[0];
                    return new CountDto(region == null ? "UNKNOWN" : region.name(),
                            region == null ? "Nieokreślony" : region.getLabel(), ((Number) row[1]).longValue());
                }).toList();
        List<CountDto> byStatus = reports.countByStatusInPeriod(from, to).stream()
                .map(row -> {
                    ReportStatus status = (ReportStatus) row[0];
                    return new CountDto(status.name(), status.name(), ((Number) row[1]).longValue());
                }).toList();
        List<MonthCountDto> byMonth = reports.countByMonthInPeriod(from, to).stream()
                .map(row -> new MonthCountDto("%04d-%02d".formatted(
                        ((Number) row[0]).intValue(), ((Number) row[1]).intValue()),
                        ((Number) row[2]).longValue()))
                .toList();
        return new TrendsDto(reports.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to),
                byArea, byRegion, byStatus, byMonth);
    }
}
