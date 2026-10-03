package pl.hubmalopolski.hub.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pl.hubmalopolski.hub.ai.ProblemClassifier;
import pl.hubmalopolski.hub.domain.ProblemReport;
import pl.hubmalopolski.hub.match.MatchmakingService;
import pl.hubmalopolski.hub.repo.ChallengeAreaRepository;
import pl.hubmalopolski.hub.repo.ProblemReportRepository;

/**
 * Modul I: zglos problem -> dostan dopasowane innowacje.
 */
@Controller
public class MatchController {

    private final MatchmakingService matchmaking;
    private final ProblemReportRepository reports;
    private final ProblemClassifier classifier;
    private final ChallengeAreaRepository areas;

    public MatchController(MatchmakingService matchmaking, ProblemReportRepository reports,
                           ProblemClassifier classifier, ChallengeAreaRepository areas) {
        this.matchmaking = matchmaking;
        this.reports = reports;
        this.classifier = classifier;
        this.areas = areas;
    }

    @GetMapping("/match")
    public String form() {
        return "match_form";
    }

    @PostMapping("/match")
    public String match(@RequestParam String description,
                        @RequestParam(required = false) String region,
                        @RequestParam(required = false) String authorName,
                        Model model) {
        ProblemReport report = new ProblemReport(description, region, authorName);
        report.setArea(classifier.classify(description, areas.findAll()));
        reports.save(report);

        model.addAttribute("report", report);
        model.addAttribute("results", matchmaking.match(description));
        return "match_results";
    }
}
