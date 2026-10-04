package pl.hubmalopolski.hub.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.hubmalopolski.hub.domain.Resource;
import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByPublishedTrueOrderByNameAsc();
}
