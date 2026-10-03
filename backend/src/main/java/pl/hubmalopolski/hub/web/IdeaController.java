package pl.hubmalopolski.hub.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import pl.hubmalopolski.hub.ai.IdeaAssistant;
import pl.hubmalopolski.hub.domain.Idea;
import pl.hubmalopolski.hub.repo.IdeaRepository;

import java.util.Map;

/**
 * Modul III: fiszki pomyslów + asystent AI.
 */
@Controller
@RequestMapping("/pomysly")
public class IdeaController {

    private final IdeaRepository ideas;
    private final IdeaAssistant assistant;

    public IdeaController(IdeaRepository ideas, IdeaAssistant assistant) {
        this.ideas = ideas;
        this.assistant = assistant;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("ideas", ideas.findAllByOrderByCreatedAtDesc());
        return "ideas";
    }

    @PostMapping
    public String create(@RequestParam String title,
                         @RequestParam(required = false) String essence,
                         @RequestParam(required = false) String targetGroup,
                         @RequestParam(required = false) String stage,
                         @RequestParam(required = false) String description) {
        ideas.save(new Idea(title, essence, targetGroup, stage, description));
        return "redirect:/pomysly";
    }

    @PostMapping("/asystent")
    @ResponseBody
    public Map<String, String> coach(@RequestParam String message) {
        try {
            return Map.of("reply", assistant.coach(message));
        } catch (Exception e) {
            return Map.of("reply", "Asystent AI jest teraz niedostepny (brak klucza API). Sprobuj pozniej.");
        }
    }
}
