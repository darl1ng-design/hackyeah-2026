package pl.hubmalopolski.hub.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.hubmalopolski.hub.domain.IdeaReply;

import java.util.List;

public interface IdeaReplyRepository extends JpaRepository<IdeaReply, Long> {
    List<IdeaReply> findByIdeaIdOrderByCreatedAtAscIdAsc(Long ideaId);
}
