package pl.hubmalopolski.hub.web;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.hubmalopolski.hub.middleman.MiddlemanService;
import pl.hubmalopolski.hub.workflow.HubWorkflowDto;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class MiddlemanController {
    public record PlanRequest(@Positive long innovationId, @NotBlank @Size(max = 200) String institution,
                             @NotBlank @Size(max = 500) String targetGroup,
                             @NotBlank @Size(max = 4000) String need,
                             @Size(max = 4000) String constraints) {}
    private final MiddlemanService middleman;

    public MiddlemanController(MiddlemanService middleman) { this.middleman = middleman; }

    @PostMapping("/middleman/plans")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ResponseEntity<HubWorkflowDto> generate(@RequestBody @Valid PlanRequest request,
            Authentication authentication) {
        HubWorkflowDto saved = middleman.generate(authentication.getName(), request.innovationId(),
                request.institution(), request.targetGroup(), request.need(), request.constraints());
        return ResponseEntity.created(URI.create("/api/v1/middleman/plans/" + saved.id())).body(saved);
    }

    @GetMapping("/middleman/plans")
    public List<HubWorkflowDto> mine(Authentication authentication) {
        return middleman.mine(authentication.getName());
    }

    @GetMapping("/middleman/plans/{id}")
    public HubWorkflowDto get(@PathVariable long id, Authentication authentication) {
        return middleman.get(id, authentication.getName());
    }

    @GetMapping("/staff/middleman/plans")
    public List<HubWorkflowDto> staffList(Authentication authentication) {
        return middleman.staffList(authentication.getName());
    }
}
