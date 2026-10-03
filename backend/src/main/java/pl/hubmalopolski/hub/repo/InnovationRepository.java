package pl.hubmalopolski.hub.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.hubmalopolski.hub.domain.Innovation;

import java.util.List;

public interface InnovationRepository extends JpaRepository<Innovation, Long> {

    /**
     * Dopasowanie slow kluczowych (LIKE) — uzupelnia wyszukiwanie wektorowe (hybrid).
     */
    @Query("select i from Innovation i where lower(i.title) like lower(concat('%', :term, '%'))"
            + " or lower(i.summary) like lower(concat('%', :term, '%'))"
            + " or lower(coalesce(i.description, '')) like lower(concat('%', :term, '%'))"
            + " or lower(coalesce(i.targetGroup, '')) like lower(concat('%', :term, '%'))")
    List<Innovation> searchKeyword(String term, Pageable pageable);

    /**
     * BM25 (Timescale pg_textsearch): <@> returns negative scores, lower = better.
     * The query is ASCII-folded with the same translate() the search_field trigger uses
     * (chr() codes keep this file pure ASCII), so 'rampa' matches a stored 'ramp' form.
     */
    @Query(value = "select id, -(search_field <@> to_bm25query("
            + " translate(cast(:q as text),"
            + " chr(261)||chr(263)||chr(281)||chr(322)||chr(324)||chr(243)||chr(347)||chr(378)||chr(380)"
            + "||chr(260)||chr(262)||chr(280)||chr(321)||chr(323)||chr(211)||chr(346)||chr(377)||chr(379),"
            + " 'acelnoszzACELNOSZZ'), 'innovation_bm25_idx')) as score"
            + " from innovation"
            + " order by search_field <@> to_bm25query("
            + " translate(cast(:q as text),"
            + " chr(261)||chr(263)||chr(281)||chr(322)||chr(324)||chr(243)||chr(347)||chr(378)||chr(380)"
            + "||chr(260)||chr(262)||chr(280)||chr(321)||chr(323)||chr(211)||chr(346)||chr(377)||chr(379),"
            + " 'acelnoszzACELNOSZZ'), 'innovation_bm25_idx')"
            + " limit :k", nativeQuery = true)
    List<Object[]> searchBm25(String q, int k);
}
