package pl.hubmalopolski.hub.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pl.hubmalopolski.hub.match.MatchmakingService;
import pl.hubmalopolski.hub.domain.Innovation;
import pl.hubmalopolski.hub.repo.ChallengeAreaRepository;
import pl.hubmalopolski.hub.repo.InnovationRepository;
import pl.hubmalopolski.hub.repo.IdeaRepository;
import pl.hubmalopolski.hub.repo.ProblemReportRepository;

/** Modul VI: panel administratora — zgloszenia, dodawanie innowacji (indeksowane wektorowo). */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ProblemReportRepository reports;
    private final InnovationRepository innovations;
    private final IdeaRepository ideas;
    private final ChallengeAreaRepository areas;
    private final MatchmakingService matchmaking;

    public AdminController(ProblemReportRepository reports, InnovationRepository innovations,
                           IdeaRepository ideas, ChallengeAreaRepository areas, MatchmakingService matchmaking) {
        this.reports = reports; this.innovations = innovations; this.ideas = ideas;
        this.areas = areas; this.matchmaking = matchmaking;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("reports", reports.findAllByOrderByCreatedAtDesc());
        model.addAttribute("ideas", ideas.findAllByOrderByCreatedAtDesc());
        model.addAttribute("innovations", innovations.findAll());
        model.addAttribute("areas", areas.findAll());
        return "admin/dashboard";
    }

    @PostMapping("/innowacje")
    public String addInnovation(@RequestParam String title,
                                @RequestParam(required = false) String summary,
                                @RequestParam(required = false) String description,
                                @RequestParam(required = false) String targetGroup,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) String region,
                                @RequestParam(required = false) Long areaId) {
        Innovation in = new Innovation(title, summary, description, targetGroup,
                status != null ? status : "ROZWOJ", region, null);
        if (areaId != null) in.setArea(areas.findById(areaId).orElse(null));
        innovations.save(in);
        try { matchmaking.index(in); innovations.save(in); } catch (Exception ignored) {}
        return "redirect:/admin";
    }

    @PostMapping("/zgloszenia/{id}/status")
    public String setStatus(@org.springframework.web.bind.annotation.PathVariable Long id,
                            @RequestParam String status) {
        reports.findById(id).ifPresent(r -> r.setStatus(status));
        return "redirect:/admin";
    }
}
