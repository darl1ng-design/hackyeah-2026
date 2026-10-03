package pl.hubmalopolski.hub.catalog;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import pl.hubmalopolski.hub.domain.Innovation;
import pl.hubmalopolski.hub.domain.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class InnovationFilters {
    private InnovationFilters() {}

    public static Specification<Innovation> matching(Long areaId, String q, Region region) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (areaId != null) predicates.add(cb.equal(root.get("area").get("id"), areaId));
            if (region != null) predicates.add(cb.equal(root.get("region"), region));
            if (q != null && !q.isBlank()) {
                String escaped = q.trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                String pattern = "%" + escaped + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("summary")), pattern, '\\'),
                        cb.like(cb.lower(root.get("description")), pattern, '\\'),
                        cb.like(cb.lower(root.get("targetGroup")), pattern, '\\')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
