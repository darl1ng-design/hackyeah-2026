package pl.hubmalopolski.hub.match;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Offline metrics for comparing matchmaking output against expert-labelled cases. */
public final class MatchQualityEvaluator {
    private MatchQualityEvaluator() {}

    public record CaseResult(String caseId, List<String> expectedIds, List<String> retrievedIds) {
        public CaseResult {
            if (caseId == null || caseId.isBlank()) throw new IllegalArgumentException("caseId is required");
            if (expectedIds == null || expectedIds.isEmpty()) {
                throw new IllegalArgumentException("At least one expected innovation is required for " + caseId);
            }
            expectedIds = clean(expectedIds);
            retrievedIds = clean(retrievedIds == null ? List.of() : retrievedIds);
            if (expectedIds.isEmpty()) throw new IllegalArgumentException("Expected innovation IDs are blank for " + caseId);
        }

        private static List<String> clean(List<String> values) {
            return values.stream().filter(value -> value != null && !value.isBlank())
                    .map(String::trim).distinct().toList();
        }
    }

    public record Metrics(int cases, int casesWithHits, double recallAtFive, double mrrAtFive,
                          List<String> missedCaseIds) {}

    /** Macro-average Recall@5 and MRR@5; duplicate retrieved IDs count only once. */
    public static Metrics evaluate(List<CaseResult> cases) {
        if (cases == null || cases.isEmpty()) throw new IllegalArgumentException("At least one case is required");
        double recall = 0;
        double reciprocalRank = 0;
        int hits = 0;
        List<String> missed = new ArrayList<>();
        Set<String> caseIds = new LinkedHashSet<>();
        for (CaseResult result : cases) {
            if (!caseIds.add(result.caseId())) throw new IllegalArgumentException("Duplicate caseId: " + result.caseId());
            Set<String> expected = Set.copyOf(result.expectedIds());
            List<String> topFive = result.retrievedIds().stream().limit(5).toList();
            long relevantFound = topFive.stream().filter(expected::contains).distinct().count();
            recall += (double) relevantFound / expected.size();
            int rank = -1;
            for (int i = 0; i < topFive.size(); i++) {
                if (expected.contains(topFive.get(i))) { rank = i + 1; break; }
            }
            if (rank > 0) {
                hits++;
                reciprocalRank += 1.0 / rank;
            } else {
                missed.add(result.caseId());
            }
        }
        return new Metrics(cases.size(), hits, recall / cases.size(), reciprocalRank / cases.size(), List.copyOf(missed));
    }
}
