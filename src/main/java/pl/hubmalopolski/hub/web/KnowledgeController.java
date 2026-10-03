package pl.hubmalopolski.hub.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import pl.hubmalopolski.hub.repo.ChallengeAreaRepository;
import pl.hubmalopolski.hub.repo.InnovationRepository;

/** Modul II: Zasobnik wiedzy — obszary wyzwan + biblioteka innowacji. */
@Controller
public class KnowledgeController {

    private final InnovationRepository innovations;
    private final ChallengeAreaRepository areas;

    public KnowledgeController(InnovationRepository innovations, ChallengeAreaRepository areas) {
        this.innovations = innovations; this.areas = areas;
    }

    @GetMapping("/wiedza")
    public String knowledge(Model model) {
        model.addAttribute("areas", areas.findAll());
        model.addAttribute("innovations", innovations.findAll());
        return "knowledge";
    }
}
