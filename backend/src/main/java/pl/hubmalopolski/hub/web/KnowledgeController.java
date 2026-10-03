package pl.hubmalopolski.hub.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import pl.hubmalopolski.hub.repo.ChallengeAreaRepository;
import pl.hubmalopolski.hub.repo.InnovationRepository;
import pl.hubmalopolski.hub.repo.ResourceRepository;

/**
 * Modul II: Zasobnik wiedzy — obszary wyzwan + biblioteka innowacji + zasoby ROPS.
 */
@Controller
public class KnowledgeController {

    private final InnovationRepository innovations;
    private final ChallengeAreaRepository areas;
    private final ResourceRepository resources;

    public KnowledgeController(InnovationRepository innovations, ChallengeAreaRepository areas,
                               ResourceRepository resources) {
        this.innovations = innovations;
        this.areas = areas;
        this.resources = resources;
    }

    @GetMapping("/wiedza")
    public String knowledge(Model model) {
        model.addAttribute("areas", areas.findAll());
        model.addAttribute("innovations", innovations.findAll());
        model.addAttribute("resources", resources.findAll());
        return "knowledge";
    }
}
