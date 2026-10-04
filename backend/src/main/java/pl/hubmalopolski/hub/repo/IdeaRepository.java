package pl.hubmalopolski.hub.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.hubmalopolski.hub.domain.Idea;
import pl.hubmalopolski.hub.domain.IdeaModerationStatus;

import java.util.List;

public interface IdeaRepository extends JpaRepository<Idea, Long> {
    List<Idea> findAllByOrderByCreatedAtDesc();
    List<Idea> findByModerationStatusOrderByCreatedAtDesc(IdeaModerationStatus status);
    List<Idea> findByOwnerEmailOrderByCreatedAtDesc(String email);
}
