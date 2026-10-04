package pl.hubmalopolski.hub.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import pl.hubmalopolski.hub.domain.Innovation;
import pl.hubmalopolski.hub.match.MatchmakingService;
import pl.hubmalopolski.hub.repo.InnovationRepository;

/**
 * Data comes from Flyway migrations (V4 challenge areas, V5 real ROPS innovations).
 * This runner only ensures every innovation is embedded into the pgvector store.
 */
@Component
public class SeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final InnovationRepository innovations;
    private final MatchmakingService matchmaking;

    @Value("${hub.seed-vectors:true}")
    private boolean seedVectors;

    public SeedRunner(InnovationRepository innovations, MatchmakingService matchmaking) {
        this.innovations = innovations;
        this.matchmaking = matchmaking;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedVectors) return;
        int done = 0, failed = 0;
        for (Innovation in : innovations.findAll()) {
            if (!in.isPublished()) continue;
            if (in.getVectorId() != null) continue;
            try {
                matchmaking.index(in);
                innovations.save(in);
                done++;
            } catch (Exception e) {
                failed++;
                log.warn("Embedding unavailable ({}). Start llama-server embedding model, then restart.",
                        e.getMessage());
                break;
            }
        }
        log.info("Wektoryzacja: {} nowych indeksow, {} bledow, lacznie {} innowacji",
                done, failed, innovations.count());
    }
}
