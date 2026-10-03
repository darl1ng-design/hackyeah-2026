package pl.hubmalopolski.hub.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.hubmalopolski.hub.domain.Innovation;

import java.util.List;

public interface InnovationRepository extends JpaRepository<Innovation, Long> {

    /** Dopasowanie slow kluczowych (LIKE) — uzupelnia wyszukiwanie wektorowe (hybrid). */
    @Query("select i from Innovation i where lower(i.title) like lower(concat('%', :term, '%'))"
         + " or lower(i.summary) like lower(concat('%', :term, '%'))"
         + " or lower(coalesce(i.description, '')) like lower(concat('%', :term, '%'))"
         + " or lower(coalesce(i.targetGroup, '')) like lower(concat('%', :term, '%'))")
    List<Innovation> searchKeyword(String term, Pageable pageable);
}
