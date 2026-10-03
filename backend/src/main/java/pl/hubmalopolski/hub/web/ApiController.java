package pl.hubmalopolski.hub.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pl.hubmalopolski.hub.ai.IdeaAssistant;
import pl.hubmalopolski.hub.domain.ChallengeArea;
import pl.hubmalopolski.hub.domain.Idea;
import pl.hubmalopolski.hub.domain.Innovation;
import pl.hubmalopolski.hub.domain.ProblemReport;
import pl.hubmalopolski.hub.domain.Resource;
import pl.hubmalopolski.hub.ai.ProblemClassifier;
import pl.hubmalopolski.hub.match.MatchResult;
import pl.hubmalopolski.hub.match.MatchmakingService;
import pl.hubmalopolski.hub.repo.ChallengeAreaRepository;
import pl.hubmalopolski.hub.repo.IdeaRepository;
import pl.hubmalopolski.hub.repo.InnovationRepository;
import pl.hubmalopolski.hub.repo.ProblemReportRepository;
import pl.hubmalopolski.hub.repo.ResourceRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST API v1 dla frontendu (Swagger: /swagger-ui.html, spec: /v3/api-docs).
 * DTO-recordy zamiast encji — stabilny kontrakt niezalezny od modelu JPA.
 */
@RestController
@RequestMapping("/api/v1")
public class ApiController {

    // ---- DTO ----
    public record AreaDto(Long id, String name, String description) {}
    public record InnovationDto(Long id, String title, String summary, String description,
                                String targetGroup, String status, String region,
                                String videoUrl, String sourceUrl, AreaDto area) {}
    public record InnovationRequest(@jakarta.validation.constraints.NotBlank String title,
                                    String summary, String description, String targetGroup,
                                    String status, String region, String sourceUrl, Long areaId) {}
    public record ResourceDto(Long id, String name, String url, String kind) {}
    public record IdeaDto(Long id, String title, String essence, String targetGroup,
                          String stage, String description, Instant createdAt) {}
    public record IdeaRequest(@jakarta.validation.constraints.NotBlank String title,
                              String essence, String targetGroup, String stage, String description) {}
    public record MatchRequest(@jakarta.validation.constraints.NotBlank String description,
                               String region, String authorName) {}
    public record MatchItemDto(InnovationDto innovation, String why, double similarity) {}
    public record MatchResponse(Long reportId, String reportStatus, AreaDto area,
                                List<MatchItemDto> matches) {}
    public record ReportDto(Long id, String description, String region, String authorName,
                            String status, AreaDto area, Instant createdAt) {}
    public record StatusRequest(@jakarta.validation.constraints.NotBlank String status) {}
    public record AssistantRequest(@jakarta.validation.constraints.NotBlank String message) {}
    public record AssistantReply(String reply) {}

    private final InnovationRepository innovations;
    private final ChallengeAreaRepository areas;
    private final ResourceRepository resources;
    private final IdeaRepository ideas;
    private final ProblemReportRepository reports;
    private final MatchmakingService matchmaking;
    private final ProblemClassifier classifier;
    private final IdeaAssistant assistant;

    public ApiController(InnovationRepository innovations, ChallengeAreaRepository areas,
                         ResourceRepository resources, IdeaRepository ideas,
                         ProblemReportRepository reports, MatchmakingService matchmaking,
                         ProblemClassifier classifier, IdeaAssistant assistant) {
        this.innovations = innovations; this.areas = areas; this.resources = resources;
        this.ideas = ideas; this.reports = reports; this.matchmaking = matchmaking;
        this.classifier = classifier; this.assistant = assistant;
    }

    // ---- biblioteka / wiedza ----

    @GetMapping("/innovations")
    public List<InnovationDto> listInnovations(@RequestParam(required = false) Long areaId,
                                               @RequestParam(required = false) String q) {
        String needle = q == null ? null : q.toLowerCase();
        return innovations.findAll().stream()
                .filter(i -> areaId == null || (i.getArea() != null && areaId.equals(i.getArea().getId())))
                .filter(i -> needle == null || matches(i, needle))
                .map(ApiController::toDto).toList();
    }

    @GetMapping("/innovations/{id}")
    public ResponseEntity<InnovationDto> getInnovation(@PathVariable Long id) {
        return innovations.findById(id)
                .map(i -> ResponseEntity.ok(toDto(i)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/areas")
    public List<AreaDto> listAreas() {
        return areas.findAll().stream().map(ApiController::toDto).toList();
    }

    @GetMapping("/resources")
    public List<ResourceDto> listResources() {
        return resources.findAll().stream()
                .map(r -> new ResourceDto(r.getId(), r.getName(), r.getUrl(), r.getKind())).toList();
    }

    // ---- matchmaking (modul I) ----

        @PostMapping("/matches")
        public MatchResponse match(@RequestBody @jakarta.validation.Valid MatchRequest req) {
            // klasyfikacja obszaru i matchmaking dekoduja tokeny na CPU — dwa
            // zapytania do llama.cpp jednoczesnie (--parallel 2) zamiast lacznie
            ProblemReport report = new ProblemReport(req.description(), req.region(), req.authorName());
            CompletableFuture<ChallengeArea> area =
                    CompletableFuture.supplyAsync(() -> classifier.classify(req.description(), areas.findAll()));
            List<MatchItemDto> items = matchmaking.match(req.description()).stream()
                    .map(m -> new MatchItemDto(toDto(m.innovation()), m.why(), m.score()))
                    .toList();
            report.setArea(area.join());
            reports.save(report);
            return new MatchResponse(report.getId(), report.getStatus(), toDto(report.getArea()), items);
        }

        @GetMapping("/reports")
        public List<ReportDto> listReports() {
            return reports.findAllByOrderByCreatedAtDesc().stream()
                    .map(r -> new ReportDto(r.getId(), r.getDescription(), r.getRegion(),
                            r.getAuthorName(), r.getStatus(), toDto(r.getArea()), r.getCreatedAt()))
                    .toList();
        }

        // ---- admin (modul VI) — wymagana rola ADMIN (SecurityConfig) ----

        @PostMapping("/admin/innovations")
        public ResponseEntity<InnovationDto> addInnovation(@RequestBody @jakarta.validation.Valid InnovationRequest req) {
            Innovation in = new Innovation(req.title(), req.summary(), req.description(),
                    req.targetGroup(), req.status() != null ? req.status() : "ROZWOJ",
                    req.region(), null);
            in.setSourceUrl(req.sourceUrl());
            if (req.areaId() != null) in.setArea(areas.findById(req.areaId()).orElse(null));
            innovations.save(in);
            try { matchmaking.index(in); innovations.save(in); } catch (Exception ignored) {}
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(in));
        }

        @PatchMapping("/admin/reports/{id}/status")
        public ResponseEntity<ReportDto> setReportStatus(@PathVariable Long id,
                                                         @RequestBody @jakarta.validation.Valid StatusRequest req) {
            return reports.findById(id)
                    .map(r -> { r.setStatus(req.status()); reports.save(r);
                            return ResponseEntity.ok(new ReportDto(r.getId(), r.getDescription(),
                                    r.getRegion(), r.getAuthorName(), r.getStatus(),
                                    toDto(r.getArea()), r.getCreatedAt())); })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }

    // ---- fiszki pomyslow (modul III) ----

    @GetMapping("/ideas")
    public List<IdeaDto> listIdeas() {
        return ideas.findAllByOrderByCreatedAtDesc().stream().map(ApiController::toDto).toList();
    }

    @PostMapping("/ideas")
    public ResponseEntity<IdeaDto> createIdea(@RequestBody @jakarta.validation.Valid IdeaRequest req) {
        Idea saved = ideas.save(new Idea(req.title(), req.essence(), req.targetGroup(),
                req.stage(), req.description()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PostMapping("/ideas/assistant")
    public AssistantReply assistant(@RequestBody @jakarta.validation.Valid AssistantRequest req) {
        try {
            return new AssistantReply(assistant.coach(req.message()));
        } catch (Exception e) {
            return new AssistantReply("Asystent AI jest teraz niedostepny. Sprobuj pozniej.");
        }
    }

    // ---- mappery ----

    private static boolean matches(Innovation i, String needle) {
        return contains(i.getTitle(), needle) || contains(i.getSummary(), needle)
                || contains(i.getDescription(), needle) || contains(i.getTargetGroup(), needle);
    }

    private static boolean contains(String s, String needle) {
        return s != null && s.toLowerCase().contains(needle);
    }

    static InnovationDto toDto(Innovation i) {
        return new InnovationDto(i.getId(), i.getTitle(), i.getSummary(), i.getDescription(),
                i.getTargetGroup(), i.getStatus(), i.getRegion(), i.getVideoUrl(),
                i.getSourceUrl(), toDto(i.getArea()));
    }

    static AreaDto toDto(ChallengeArea a) {
        return a == null ? null : new AreaDto(a.getId(), a.getName(), a.getDescription());
    }

    static IdeaDto toDto(Idea i) {
        return new IdeaDto(i.getId(), i.getTitle(), i.getEssence(), i.getTargetGroup(),
                i.getStage(), i.getDescription(), i.getCreatedAt());
    }
}