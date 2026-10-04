package pl.hubmalopolski.hub.web;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.hubmalopolski.hub.testing.InnovationTestingService;
import pl.hubmalopolski.hub.workflow.HubWorkflowDto;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InnovationTestingController {
    public record FeedbackRequest(@AssertTrue Boolean interested, @Min(1) @Max(5) Integer rating,
                                 @Size(max = 3000) String feedback, @Size(max = 2000) String suggestion) {}
    private final InnovationTestingService testing;

    public InnovationTestingController(InnovationTestingService testing) { this.testing = testing; }

    @PostMapping("/innovations/{id}/tester-feedback")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ResponseEntity<HubWorkflowDto> submit(@PathVariable long id, @RequestBody @Valid FeedbackRequest request,
                                                  Authentication authentication) {
        HubWorkflowDto saved = testing.submit(id, authentication.getName(),
                Boolean.TRUE.equals(request.interested()), request.rating(), request.feedback(), request.suggestion());
        return ResponseEntity.created(URI.create("/api/v1/innovations/" + id + "/tester-feedback/mine"))
                .body(saved);
    }

    @GetMapping("/innovations/{id}/tester-feedback/mine")
    public HubWorkflowDto mine(@PathVariable long id, Authentication authentication) {
        return testing.mine(id, authentication.getName());
    }

    @GetMapping("/staff/tester-feedback")
    public List<HubWorkflowDto> staffList(Authentication authentication) {
        return testing.staffList(authentication.getName());
    }
}
