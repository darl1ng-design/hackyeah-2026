package pl.hubmalopolski.hub.web;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pl.hubmalopolski.hub.catalog.KnowledgeService;
import pl.hubmalopolski.hub.repo.InnovationRepository;
import pl.hubmalopolski.hub.repo.ResourceRepository;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@SecurityRequirement(name = "cookieAuth")
public class KnowledgeAdminController {
    private final KnowledgeService knowledge;
    private final InnovationRepository innovations;
    private final ResourceRepository resources;

    public KnowledgeAdminController(KnowledgeService knowledge, InnovationRepository innovations,
                                    ResourceRepository resources) {
        this.knowledge = knowledge;
        this.innovations = innovations;
        this.resources = resources;
    }

    @GetMapping("/innovations")
    public ApiController.PageDto<ApiController.InnovationDto> listInnovations(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page >= 0, size 1..100");
        }
        Page<ApiController.InnovationDto> result = innovations.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .map(ApiController::toDto);
        return ApiController.PageDto.from(result);
    }

    @PutMapping("/innovations/{id}")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ApiController.InnovationDto updateInnovation(@PathVariable long id,
            @RequestBody @Valid KnowledgeService.InnovationUpdate update) {
        return ApiController.toDto(knowledge.updateInnovation(id, update));
    }

    @GetMapping("/innovations/{id}")
    public ResponseEntity<ApiController.InnovationDto> getInnovation(@PathVariable long id) {
        return innovations.findById(id).map(ApiController::toDto)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/resources")
    public List<ApiController.ResourceDto> listResources() {
        return resources.findAll(Sort.by("name", "id")).stream().map(ApiController::toDto).toList();
    }

    @PostMapping("/resources")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ResponseEntity<ApiController.ResourceDto> createResource(
            @RequestBody @Valid KnowledgeService.ResourceUpdate update) {
        var saved = knowledge.createResource(update);
        return ResponseEntity.created(URI.create("/api/v1/admin/resources/" + saved.getId()))
                .body(ApiController.toDto(saved));
    }

    @PutMapping("/resources/{id}")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ApiController.ResourceDto updateResource(@PathVariable long id,
            @RequestBody @Valid KnowledgeService.ResourceUpdate update) {
        return ApiController.toDto(knowledge.updateResource(id, update));
    }

    @GetMapping("/resources/{id}")
    public ResponseEntity<ApiController.ResourceDto> getResource(@PathVariable long id) {
        return resources.findById(id).map(ApiController::toDto)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/areas")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ResponseEntity<ApiController.AreaDto> createArea(
            @RequestBody @Valid KnowledgeService.AreaUpdate update) {
        var saved = knowledge.createArea(update);
        return ResponseEntity.created(URI.create("/api/v1/areas/" + saved.getId()))
                .body(ApiController.toDto(saved));
    }

    @PutMapping("/areas/{id}")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ApiController.AreaDto updateArea(@PathVariable long id,
            @RequestBody @Valid KnowledgeService.AreaUpdate update) {
        return ApiController.toDto(knowledge.updateArea(id, update));
    }
}
