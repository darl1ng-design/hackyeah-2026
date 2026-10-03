package pl.hubmalopolski.hub.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.hubmalopolski.hub.domain.Resource;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}