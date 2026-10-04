package pl.hubmalopolski.hub.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.hubmalopolski.hub.domain.ChallengeArea;
import pl.hubmalopolski.hub.domain.Innovation;
import pl.hubmalopolski.hub.domain.InnovationStatus;
import pl.hubmalopolski.hub.domain.Region;
import pl.hubmalopolski.hub.domain.Resource;
import pl.hubmalopolski.hub.domain.ResourceKind;
import pl.hubmalopolski.hub.match.MatchmakingService;
import pl.hubmalopolski.hub.repo.ChallengeAreaRepository;
import pl.hubmalopolski.hub.repo.InnovationRepository;
import pl.hubmalopolski.hub.repo.ResourceRepository;

@Service
public class KnowledgeService {
    public record InnovationUpdate(@NotBlank @Size(max = 255) String title,
                                   @Size(max = 1000) String summary, String description,
                                   @Size(max = 255) String targetGroup,
                                   @NotNull InnovationStatus status, Region region,
                                   @Size(max = 500) @Pattern(regexp = "https?://.*") String videoUrl,
                                   @Size(max = 500) @Pattern(regexp = "https?://.*") String sourceUrl,
                                   Long areaId, @NotNull Boolean published) {}
    public record ResourceUpdate(@NotBlank @Size(max = 255) String name,
                                 @NotBlank @Size(max = 500) @Pattern(regexp = "https?://.*") String url,
                                 @NotNull ResourceKind kind, @NotNull Boolean published) {}
    public record AreaUpdate(@NotBlank @Size(max = 255) String name,
                             @Size(max = 1000) String description) {}

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);
    private final InnovationRepository innovations;
    private final ResourceRepository resources;
    private final ChallengeAreaRepository areas;
    private final MatchmakingService matchmaking;

    public KnowledgeService(InnovationRepository innovations, ResourceRepository resources,
                            ChallengeAreaRepository areas, MatchmakingService matchmaking) {
        this.innovations = innovations;
        this.resources = resources;
        this.areas = areas;
        this.matchmaking = matchmaking;
    }

    @Transactional
    public Innovation updateInnovation(long id, InnovationUpdate update) {
        Innovation innovation = innovations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        innovation.setTitle(update.title().trim());
        innovation.setSummary(update.summary());
        innovation.setDescription(update.description());
        innovation.setTargetGroup(update.targetGroup());
        innovation.setStatus(update.status());
        innovation.setRegion(update.region());
        innovation.setVideoUrl(update.videoUrl());
        innovation.setSourceUrl(update.sourceUrl());
        innovation.setArea(update.areaId() == null ? null : areas.findById(update.areaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown areaId")));
        innovation.setPublished(update.published());
        if (innovation.isPublished()) {
            try { matchmaking.index(innovation); }
            catch (Exception e) {
                innovation.setVectorId(null);
                log.warn("Indeks innowacji {} będzie odtworzony po uruchomieniu: {}", id, e.getMessage());
            }
        } else {
            try { matchmaking.removeIndex(innovation); }
            catch (Exception e) {
                innovation.setVectorId(null);
                log.warn("Nie udało się usunąć wektora innowacji {}: {}", id, e.getMessage());
            }
        }
        return innovations.save(innovation);
    }

    @Transactional
    public Resource createResource(ResourceUpdate update) {
        return resources.save(new Resource(update.name().trim(), update.url().trim(),
                update.kind(), update.published()));
    }

    @Transactional
    public Resource updateResource(long id, ResourceUpdate update) {
        Resource resource = resources.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        resource.setName(update.name().trim());
        resource.setUrl(update.url().trim());
        resource.setKind(update.kind());
        resource.setPublished(update.published());
        return resources.save(resource);
    }

    @Transactional
    public ChallengeArea createArea(AreaUpdate update) {
        if (areas.existsByNameIgnoreCase(update.name().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Area already exists");
        }
        return areas.save(new ChallengeArea(update.name().trim(), update.description()));
    }

    @Transactional
    public ChallengeArea updateArea(long id, AreaUpdate update) {
        ChallengeArea area = areas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!area.getName().equalsIgnoreCase(update.name().trim())
                && areas.existsByNameIgnoreCase(update.name().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Area already exists");
        }
        area.setName(update.name().trim());
        area.setDescription(update.description());
        ChallengeArea saved = areas.save(area);
        for (Innovation innovation : innovations.findByAreaId(id)) {
            if (!innovation.isPublished()) continue;
            try { matchmaking.index(innovation); innovations.save(innovation); }
            catch (Exception e) {
                innovation.setVectorId(null);
                innovations.save(innovation);
                log.warn("Ponowne indeksowanie innowacji {} będzie wymagane: {}",
                        innovation.getId(), e.getMessage());
            }
        }
        return saved;
    }
}
