package pl.hubmalopolski.hub.match;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.hubmalopolski.hub.domain.ProblemReport;
import pl.hubmalopolski.hub.domain.ReportMatch;
import pl.hubmalopolski.hub.repo.ProblemReportRepository;
import pl.hubmalopolski.hub.repo.ReportMatchRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MatchReportService {
    public record SavedReport(ProblemReport report, List<ReportMatch> matches) {}

    private final ProblemReportRepository reports;
    private final ReportMatchRepository matches;

    public MatchReportService(ProblemReportRepository reports, ReportMatchRepository matches) {
        this.reports = reports;
        this.matches = matches;
    }

    @Transactional
    public SavedReport save(ProblemReport report, List<MatchResult> results) {
        ProblemReport saved = reports.save(report);
        List<ReportMatch> entries = new ArrayList<>();
        for (int index = 0; index < results.size(); index++) {
            MatchResult result = results.get(index);
            entries.add(new ReportMatch(saved, result.innovation(), index,
                    result.why(), result.score()));
        }
        matches.saveAll(entries);
        return new SavedReport(saved, entries);
    }

    @Transactional(readOnly = true)
    public Optional<SavedReport> find(Long reportId) {
        return reports.findById(reportId)
                .map(report -> new SavedReport(report, matches.findByReportIdOrderByRankOrderAsc(reportId)));
    }
}
