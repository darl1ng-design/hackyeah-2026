package pl.hubmalopolski.hub.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.hubmalopolski.hub.domain.ChallengeArea;

public interface ChallengeAreaRepository extends JpaRepository<ChallengeArea, Long> {
}
