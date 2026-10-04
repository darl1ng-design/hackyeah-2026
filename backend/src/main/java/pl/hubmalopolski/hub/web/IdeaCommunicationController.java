package pl.hubmalopolski.hub.web;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.hubmalopolski.hub.communication.IdeaCommunicationService;
import pl.hubmalopolski.hub.domain.IdeaModerationStatus;
import pl.hubmalopolski.hub.repo.IdeaRepository;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "cookieAuth")
public class IdeaCommunicationController {
    public record ReplyRequest(@NotBlank @Size(max = 4000) String body) {}

    private final IdeaCommunicationService communication;
    private final IdeaRepository ideas;

    public IdeaCommunicationController(IdeaCommunicationService communication, IdeaRepository ideas) {
        this.communication = communication;
        this.ideas = ideas;
    }

    @GetMapping("/staff/ideas")
    public List<ApiController.IdeaDto> staffIdeas(
            @RequestParam(defaultValue = "PENDING") IdeaModerationStatus status) {
        return ideas.findByModerationStatusOrderByCreatedAtDesc(status).stream()
                .map(ApiController::toDto).toList();
    }

    @PostMapping("/staff/ideas/{id}/replies")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ResponseEntity<IdeaCommunicationService.ReplyDto> reply(@PathVariable long id,
            @RequestBody @Valid ReplyRequest request, Authentication authentication) {
        var saved = communication.reply(id, authentication.getName(), request.body());
        return ResponseEntity.created(URI.create("/api/v1/ideas/" + id + "/replies"))
                .body(saved);
    }

    @GetMapping("/ideas/{id}/replies")
    public List<IdeaCommunicationService.ReplyDto> replies(@PathVariable long id,
                                                            Authentication authentication) {
        return communication.replies(id, authentication.getName());
    }

    @GetMapping("/notifications")
    public List<IdeaCommunicationService.NotificationDto> notifications(Authentication authentication) {
        return communication.notifications(authentication.getName());
    }

    @PatchMapping("/notifications/{id}/read")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public IdeaCommunicationService.NotificationDto markRead(@PathVariable long id,
                                                               Authentication authentication) {
        return communication.markRead(id, authentication.getName());
    }
}
