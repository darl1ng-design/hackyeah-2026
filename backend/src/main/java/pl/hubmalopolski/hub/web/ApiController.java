package pl.hubmalopolski.hub.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import pl.hubmalopolski.hub.ai.IdeaAssistant;
import pl.hubmalopolski.hub.ai.IdeaStoryParser;
import pl.hubmalopolski.hub.ai.IdeaAssistant.Turn;
import pl.hubmalopolski.hub.ai.IdeaAssistant.IdeaContext;
import pl.hubmalopolski.hub.catalog.InnovationFilters;
import pl.hubmalopolski.hub.communication.IdeaCommunicationService;
import pl.hubmalopolski.hub.domain.ChallengeArea;
import pl.hubmalopolski.hub.domain.Idea;
import pl.hubmalopolski.hub.domain.AppUser;
import pl.hubmalopolski.hub.domain.IdeaModerationStatus;
import pl.hubmalopolski.hub.domain.IdeaStage;
import pl.hubmalopolski.hub.domain.Innovation;
import pl.hubmalopolski.hub.domain.InnovationStatus;
import pl.hubmalopolski.hub.domain.ProblemReport;
import pl.hubmalopolski.hub.domain.Region;
import pl.hubmalopolski.hub.domain.ReportStatus;
import pl.hubmalopolski.hub.domain.Resource;
import pl.hubmalopolski.hub.domain.ResourceKind;
import pl.hubmalopolski.hub.ai.ProblemClassifier;
import pl.hubmalopolski.hub.match.MatchResult;
import pl.hubmalopolski.hub.match.MatchReportService;
import pl.hubmalopolski.hub.match.MatchmakingService;
import pl.hubmalopolski.hub.repo.ChallengeAreaRepository;
import pl.hubmalopolski.hub.repo.AppUserRepository;
import pl.hubmalopolski.hub.repo.IdeaRepository;
import pl.hubmalopolski.hub.repo.InnovationRepository;
import pl.hubmalopolski.hub.repo.ProblemReportRepository;
import pl.hubmalopolski.hub.repo.ResourceRepository;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Set;

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
                                String targetGroup, InnovationStatus status, Region region,
                                String videoUrl, String sourceUrl, AreaDto area, Instant createdAt,
                                boolean published) {}
    public record InnovationRequest(@jakarta.validation.constraints.NotBlank
                                    @jakarta.validation.constraints.Size(max = 255) String title,
                                    String summary, String description, String targetGroup,
                                    InnovationStatus status, Region region, String sourceUrl, Long areaId,
                                    Boolean published) {}
    public record ResourceDto(Long id, String name, String url, ResourceKind kind, boolean published) {}
    public record IdeaDto(Long id, String title, String essence, String targetGroup,
                          IdeaStage stage, String description, String author,
                          IdeaModerationStatus moderationStatus, Instant createdAt) {}
    public record IdeaRequest(@jakarta.validation.constraints.NotBlank
                              @jakarta.validation.constraints.Size(max = 255) String title,
                              String essence, String targetGroup, IdeaStage stage, String description) {}
    public record ModerationRequest(@jakarta.validation.constraints.NotNull IdeaModerationStatus status) {}
    public record MatchRequest(@jakarta.validation.constraints.NotBlank String description,
                               Region region, String authorName) {}
    public record MatchItemDto(InnovationDto innovation, String why, double similarity) {}
    public record MatchResponse(Long reportId, ReportStatus reportStatus, AreaDto area,
                                List<MatchItemDto> matches) {}
    public record ReportDto(Long id, String description, Region region, String authorName,
                            ReportStatus status, AreaDto area, Instant createdAt) {}
    public record StatusRequest(@jakarta.validation.constraints.NotNull ReportStatus status) {}
    public record AssistantRequest(@jakarta.validation.constraints.NotBlank
                                   @jakarta.validation.constraints.Size(max = 4000) String message,
                                   @jakarta.validation.constraints.Size(max = 20)
                                   List<@jakarta.validation.constraints.NotNull @jakarta.validation.Valid Turn> history,
                                   @jakarta.validation.Valid IdeaContext ideaContext) {}
    public record AssistantReply(String reply) {}
    public record ParseIdeaRequest(@jakarta.validation.constraints.NotBlank
                                   @jakarta.validation.constraints.Size(max = 6000) String text) {}
    public record MeDto(String username, List<String> roles) {}
    public record CsrfDto(String token, String headerName, String parameterName) {}
    public record RegionDto(Region code, String label) {}
    public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        static <T> PageDto<T> from(Page<T> result) {
            return new PageDto<>(result.getContent(), result.getNumber(), result.getSize(),
                    result.getTotalElements(), result.getTotalPages());
        }
    }

    private final InnovationRepository innovations;
    private final ChallengeAreaRepository areas;
    private final ResourceRepository resources;
    private final IdeaRepository ideas;
    private final AppUserRepository users;
    private final ProblemReportRepository reports;
    private final MatchmakingService matchmaking;
    private final MatchReportService matchReports;
    private final ProblemClassifier classifier;
    private final IdeaAssistant assistant;
    private final IdeaStoryParser storyParser;
    private final IdeaCommunicationService communication;

    public ApiController(InnovationRepository innovations, ChallengeAreaRepository areas,
                         ResourceRepository resources, IdeaRepository ideas,
                         AppUserRepository users,
                         ProblemReportRepository reports, MatchmakingService matchmaking,
                         MatchReportService matchReports, ProblemClassifier classifier, IdeaAssistant assistant, IdeaStoryParser storyParser,
                         IdeaCommunicationService communication) {
        this.innovations = innovations; this.areas = areas; this.resources = resources;
        this.ideas = ideas; this.users = users; this.reports = reports; this.matchmaking = matchmaking;
        this.matchReports = matchReports;
        this.classifier = classifier; this.assistant = assistant; this.storyParser = storyParser;
        this.communication = communication;
    }

    // ---- biblioteka / wiedza ----

    @GetMapping("/me")
    @SecurityRequirement(name = "cookieAuth")
    public MeDto me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .sorted().toList();
        return new MeDto(authentication.getName(), roles);
    }

    @GetMapping("/csrf")
    public CsrfDto csrf(@RequestAttribute("_csrf") CsrfToken token) {
        return new CsrfDto(token.getToken(), token.getHeaderName(), token.getParameterName());
    }

    @GetMapping("/innovations")
    public PageDto<InnovationDto> listInnovations(@RequestParam(required = false) Long areaId,
                                                   @RequestParam(required = false) String q,
                                                   @RequestParam(required = false) Region region,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(defaultValue = "createdAt,desc") String sort) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page >= 0, size 1..100");
        }
        String[] sortParts = sort.split(",", -1);
        if (sortParts.length != 2 || !Set.of("createdAt", "title", "status").contains(sortParts[0])
                || !(sortParts[1].equalsIgnoreCase("asc") || sortParts[1].equalsIgnoreCase("desc"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort; use field,asc|desc");
        }
        Sort.Direction direction = Sort.Direction.fromString(sortParts[1]);
        Page<InnovationDto> result = innovations.findAll(InnovationFilters.matching(areaId, q, region),
                PageRequest.of(page, size, Sort.by(direction, sortParts[0]).and(Sort.by("id"))))
                .map(ApiController::toDto);
        return PageDto.from(result);
    }

    @GetMapping("/innovations/{id}")
    public ResponseEntity<InnovationDto> getInnovation(@PathVariable Long id) {
        return innovations.findById(id)
                .filter(Innovation::isPublished)
                .map(i -> ResponseEntity.ok(toDto(i)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/areas")
    public List<AreaDto> listAreas() {
        return areas.findAll().stream().map(ApiController::toDto).toList();
    }

    @GetMapping("/regions")
    public List<RegionDto> listRegions() {
        return java.util.Arrays.stream(Region.values())
                .map(region -> new RegionDto(region, region.getLabel())).toList();
    }

    @GetMapping("/resources")
    public List<ResourceDto> listResources() {
        return resources.findByPublishedTrueOrderByNameAsc().stream()
                .map(ApiController::toDto).toList();
    }

    // ---- matchmaking (modul I) ----

        @PostMapping("/matches")
        @SecurityRequirement(name = "csrfToken")
        public MatchResponse match(@RequestBody @jakarta.validation.Valid MatchRequest req) {
            // klasyfikacja obszaru i matchmaking dekoduja tokeny na CPU — dwa
            // zapytania do llama.cpp jednoczesnie (--parallel 2) zamiast lacznie
            ProblemReport report = new ProblemReport(req.description(), req.region(), req.authorName());
            CompletableFuture<ChallengeArea> area =
                    CompletableFuture.supplyAsync(() -> classifier.classify(req.description(), areas.findAll()));
            List<MatchResult> results = matchmaking.match(req.description());
            report.setArea(area.join());
            MatchReportService.SavedReport saved = matchReports.save(report, results);
            List<MatchItemDto> items = results.stream()
                    .map(m -> new MatchItemDto(toDto(m.innovation()), m.why(), m.score()))
                    .toList();
            return new MatchResponse(saved.report().getId(), saved.report().getStatus(),
                    toDto(saved.report().getArea()), items);
        }

        @GetMapping("/matches/{reportId}")
        public ResponseEntity<MatchResponse> getMatch(@PathVariable Long reportId) {
            return matchReports.find(reportId).map(saved -> ResponseEntity.ok(new MatchResponse(
                    saved.report().getId(), saved.report().getStatus(), toDto(saved.report().getArea()),
                    saved.matches().stream().filter(entry -> entry.getInnovation().isPublished())
                            .map(entry -> new MatchItemDto(
                            toDto(entry.getInnovation()), entry.getWhy(), entry.getSimilarity())).toList())))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }

        @GetMapping("/reports")
        @SecurityRequirement(name = "cookieAuth")
        public List<ReportDto> listReports() {
            return reports.findAllByOrderByCreatedAtDesc().stream()
                    .map(r -> new ReportDto(r.getId(), r.getDescription(), r.getRegion(),
                            r.getAuthorName(), r.getStatus(), toDto(r.getArea()), r.getCreatedAt()))
                    .toList();
        }

        // ---- admin (modul VI) — wymagana rola ADMIN (SecurityConfig) ----

        @PostMapping("/admin/innovations")
        @SecurityRequirement(name = "cookieAuth")
        @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
        public ResponseEntity<InnovationDto> addInnovation(@RequestBody @jakarta.validation.Valid InnovationRequest req) {
            Innovation in = new Innovation(req.title(), req.summary(), req.description(),
                    req.targetGroup(), req.status() != null ? req.status() : InnovationStatus.ROZWOJ,
                    req.region(), null);
            in.setSourceUrl(req.sourceUrl());
            in.setPublished(Boolean.TRUE.equals(req.published()));
            if (req.areaId() != null) in.setArea(areas.findById(req.areaId()).orElse(null));
            innovations.save(in);
            if (in.isPublished()) {
                try { matchmaking.index(in); innovations.save(in); }
                catch (Exception ignored) { /* SeedRunner ponowi indeksowanie po starcie. */ }
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(in));
        }

        @PatchMapping("/admin/reports/{id}/status")
        @SecurityRequirement(name = "cookieAuth")
        @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
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
    public List<IdeaDto> listIdeas(@RequestParam(defaultValue = "false") boolean mine,
                                  Authentication authentication) {
        if (mine) {
            String email = currentEmail(authentication);
            if (email == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            return ideas.findByOwnerEmailOrderByCreatedAtDesc(email).stream()
                    .map(ApiController::toDto).toList();
        }
        return ideas.findByModerationStatusOrderByCreatedAtDesc(IdeaModerationStatus.APPROVED)
                .stream().map(ApiController::toDto).toList();
    }

    @GetMapping("/ideas/{id}")
    public ResponseEntity<IdeaDto> getIdea(@PathVariable Long id, Authentication authentication) {
        return ideas.findById(id)
                .filter(idea -> idea.getModerationStatus() == IdeaModerationStatus.APPROVED
                        || isStaff(authentication)
                        || (currentEmail(authentication) != null && idea.getOwner() != null
                            && currentEmail(authentication).equals(idea.getOwner().getEmail())))
                .map(idea -> ResponseEntity.ok(toDto(idea)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/ideas")
    @SecurityRequirement(name = "cookieAuth")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    @Transactional
    public ResponseEntity<IdeaDto> createIdea(@RequestBody @jakarta.validation.Valid IdeaRequest req,
                                             Authentication authentication) {
        AppUser owner = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Idea saved = ideas.save(new Idea(req.title(), req.essence(), req.targetGroup(),
                req.stage(), req.description(), owner.getDisplayName(), owner));
        communication.onIdeaSubmitted(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @GetMapping("/admin/ideas")
    @SecurityRequirement(name = "cookieAuth")
    public List<IdeaDto> listIdeasForModeration(@RequestParam(defaultValue = "PENDING")
                                                 IdeaModerationStatus status) {
        return ideas.findByModerationStatusOrderByCreatedAtDesc(status).stream()
                .map(ApiController::toDto).toList();
    }

    @PatchMapping("/admin/ideas/{id}/moderation")
    @SecurityRequirement(name = "cookieAuth")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ResponseEntity<IdeaDto> moderateIdea(@PathVariable Long id,
            @RequestBody @jakarta.validation.Valid ModerationRequest request) {
        return ideas.findById(id).map(idea -> {
            idea.setModerationStatus(request.status());
            return ResponseEntity.ok(toDto(ideas.save(idea)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/ideas/assistant")
    @SecurityRequirement(name = "csrfToken")
    public AssistantReply assistant(@RequestBody @jakarta.validation.Valid AssistantRequest req) {
        try {
            return new AssistantReply(assistant.coach(req.message(), req.history(), req.ideaContext()));
        } catch (Exception e) {
            return new AssistantReply("Asystent AI jest teraz niedostepny. Sprobuj pozniej.");
        }
    }

    @PostMapping("/ideas/assistant/parse")
    @SecurityRequirement(name = "csrfToken")
    public IdeaStoryParser.ParsedIdea parseIdea(@RequestBody @jakarta.validation.Valid ParseIdeaRequest req) {
        try {
            return storyParser.parse(req.text());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Asystent AI jest teraz niedostepny.");
        }
    }

    // ---- mappery ----

    static InnovationDto toDto(Innovation i) {
        return new InnovationDto(i.getId(), i.getTitle(), i.getSummary(), i.getDescription(),
                i.getTargetGroup(), i.getStatus(), i.getRegion(), i.getVideoUrl(),
                i.getSourceUrl(), toDto(i.getArea()), i.getCreatedAt(), i.isPublished());
    }

    static ResourceDto toDto(Resource r) {
        return new ResourceDto(r.getId(), r.getName(), r.getUrl(), r.getKind(), r.isPublished());
    }

    static AreaDto toDto(ChallengeArea a) {
        return a == null ? null : new AreaDto(a.getId(), a.getName(), a.getDescription());
    }

    static IdeaDto toDto(Idea i) {
        return new IdeaDto(i.getId(), i.getTitle(), i.getEssence(), i.getTargetGroup(),
                i.getStage(), i.getDescription(), i.getAuthor(), i.getModerationStatus(), i.getCreatedAt());
    }

    private static String currentEmail(Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                ? null : authentication.getName();
    }

    private static boolean isStaff(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_STAFF") || role.equals("ROLE_ADMIN"));
    }
}
