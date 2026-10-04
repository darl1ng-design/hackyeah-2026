package pl.hubmalopolski.hub.web;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.hubmalopolski.hub.grants.GrantApplicationStatus;
import pl.hubmalopolski.hub.grants.GrantCallStatus;
import pl.hubmalopolski.hub.grants.GrantFieldType;
import pl.hubmalopolski.hub.grants.GrantService;
import pl.hubmalopolski.hub.workflow.HubWorkflowDto;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class GrantController {
    public record FieldRequest(@NotBlank @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_]{0,39}") String key,
                               @NotBlank @Size(max = 160) String label, @NotNull GrantFieldType type,
                               boolean required, @Size(max = 50) List<@NotBlank @Size(max = 120) String> options) {}
    public record GrantCallRequest(@NotBlank @Size(max = 255) String title,
                                   @Size(max = 4000) String description,
                                   @NotNull Instant opensAt, @NotNull Instant closesAt,
                                   GrantCallStatus status, @NotEmpty @Size(max = 40) List<@Valid FieldRequest> fields) {}
    public record ApplicationRequest(@NotNull @Size(max = 40) Map<@NotBlank String, @NotNull @Size(max = 10000) String> answers) {}
    public record StatusRequest(@NotNull GrantApplicationStatus status) {}

    private final GrantService grants;
    public GrantController(GrantService grants) { this.grants = grants; }

    @GetMapping("/grant-calls")
    public List<HubWorkflowDto> openCalls() { return grants.openCalls(); }

    @GetMapping("/admin/grant-calls")
    public List<HubWorkflowDto> adminCalls(Authentication authentication) {
        return grants.adminCalls(authentication.getName());
    }

    @PostMapping("/admin/grant-calls")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ResponseEntity<HubWorkflowDto> createCall(@RequestBody @Valid GrantCallRequest request,
                                                     Authentication authentication) {
        HubWorkflowDto saved = grants.createCall(authentication.getName(), request.title(), request.description(),
                request.opensAt(), request.closesAt(), name(request.status()), fields(request.fields()));
        return ResponseEntity.created(URI.create("/api/v1/admin/grant-calls/" + saved.id())).body(saved);
    }

    @PutMapping("/admin/grant-calls/{id}")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public HubWorkflowDto updateCall(@PathVariable long id, @RequestBody @Valid GrantCallRequest request,
                                     Authentication authentication) {
        return grants.updateCall(id, authentication.getName(), request.title(), request.description(),
                request.opensAt(), request.closesAt(), name(request.status()), fields(request.fields()));
    }

    @PostMapping("/grant-calls/{id}/applications")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ResponseEntity<HubWorkflowDto> submitApplication(@PathVariable long id,
            @RequestBody @Valid ApplicationRequest request, Authentication authentication) {
        HubWorkflowDto saved = grants.submitApplication(id, authentication.getName(), request.answers());
        return ResponseEntity.created(URI.create("/api/v1/grant-applications?mine=true")).body(saved);
    }

    @GetMapping("/grant-applications")
    public List<HubWorkflowDto> myApplications(@RequestParam(defaultValue = "true") boolean mine,
                                               Authentication authentication) {
        if (!mine) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Only mine=true is supported");
        return grants.myApplications(authentication.getName());
    }

    @GetMapping("/staff/grant-applications")
    public List<HubWorkflowDto> staffApplications(Authentication authentication) {
        return grants.staffApplications(authentication.getName());
    }

    @PatchMapping("/staff/grant-applications/{id}/status")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public HubWorkflowDto changeApplicationStatus(@PathVariable long id, @RequestBody @Valid StatusRequest request,
                                                  Authentication authentication) {
        return grants.changeApplicationStatus(id, authentication.getName(), request.status().name());
    }

    private static List<GrantService.Field> fields(List<FieldRequest> fields) {
        return fields.stream().map(field -> new GrantService.Field(field.key(), field.label(), field.type().name(),
                field.required(), field.options())).toList();
    }
    private static String name(Enum<?> value) { return value == null ? null : value.name(); }
}
