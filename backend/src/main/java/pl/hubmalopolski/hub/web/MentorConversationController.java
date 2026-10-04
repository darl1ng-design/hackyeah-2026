package pl.hubmalopolski.hub.web;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.hubmalopolski.hub.communication.MentorConversationService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class MentorConversationController {
    public record OpenRequest(@NotBlank @Size(max = 180) String subject,
                              @NotBlank @Size(max = 4000) String body) {}
    public record MessageRequest(@NotBlank @Size(max = 4000) String body) {}
    private final MentorConversationService conversations;

    public MentorConversationController(MentorConversationService conversations) { this.conversations = conversations; }

    @PostMapping("/mentor/conversations")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ResponseEntity<MentorConversationService.ConversationDto> open(@RequestBody @Valid OpenRequest request,
            Authentication authentication) {
        var saved = conversations.open(authentication.getName(), request.subject(), request.body());
        return ResponseEntity.created(URI.create("/api/v1/mentor/conversations/" + saved.id())).body(saved);
    }

    @GetMapping("/mentor/conversations")
    public List<MentorConversationService.ConversationDto> mine(Authentication authentication) {
        return conversations.mine(authentication.getName());
    }

    @GetMapping("/mentor/conversations/{id}")
    public MentorConversationService.ConversationDto get(@PathVariable long id, Authentication authentication) {
        return conversations.get(id, authentication.getName());
    }

    @PostMapping("/mentor/conversations/{id}/messages")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ResponseEntity<MentorConversationService.ConversationDto> message(@PathVariable long id,
            @RequestBody @Valid MessageRequest request, Authentication authentication) {
        return ResponseEntity.status(201).body(conversations.message(id, authentication.getName(), request.body()));
    }

    @GetMapping("/staff/mentor/conversations")
    public List<MentorConversationService.ConversationDto> staffList(Authentication authentication) {
        return conversations.staffList(authentication.getName());
    }

    @GetMapping("/staff/mentor/conversations/{id}")
    public MentorConversationService.ConversationDto staffGet(@PathVariable long id, Authentication authentication) {
        return conversations.get(id, authentication.getName());
    }

    @PostMapping("/staff/mentor/conversations/{id}/messages")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ResponseEntity<MentorConversationService.ConversationDto> reply(@PathVariable long id,
            @RequestBody @Valid MessageRequest request, Authentication authentication) {
        return ResponseEntity.status(201).body(conversations.reply(id, authentication.getName(), request.body()));
    }
}
