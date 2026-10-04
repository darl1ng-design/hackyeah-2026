package pl.hubmalopolski.hub.match;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchQualityEvaluatorTests {
    @Test
    void calculatesRecallAndReciprocalRankAtFiveAcrossCases() {
        var metrics = MatchQualityEvaluator.evaluate(List.of(
                new MatchQualityEvaluator.CaseResult("first", List.of("a", "b"),
                        List.of("a", "x", "b")),
                new MatchQualityEvaluator.CaseResult("fifth", List.of("c"),
                        List.of("x", "y", "z", "w", "c")),
                new MatchQualityEvaluator.CaseResult("miss", List.of("d"),
                        List.of("x", "y"))));

        assertEquals(3, metrics.cases());
        assertEquals(4.0 / 6.0, metrics.recallAtFive(), 0.00001);
        assertEquals((1.0 + 0.2 + 0.0) / 3.0, metrics.mrrAtFive(), 0.00001);
        assertEquals(List.of("miss"), metrics.missedCaseIds());
    }

    @Test
    void rejectsCasesWithoutExpectedLabelsAndDeduplicatesCandidates() {
        var metrics = MatchQualityEvaluator.evaluate(List.of(
                new MatchQualityEvaluator.CaseResult("dup", List.of("a"),
                        List.of("x", "a", "a", "b", "c", "d"))));

        assertEquals(1.0, metrics.recallAtFive(), 0.00001);
        assertEquals(0.5, metrics.mrrAtFive(), 0.00001);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> MatchQualityEvaluator.evaluate(List.of(
                        new MatchQualityEvaluator.CaseResult("empty", List.of(), List.of("a")))));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> MatchQualityEvaluator.evaluate(List.of(
                        new MatchQualityEvaluator.CaseResult("duplicate", List.of("a"), List.of()),
                        new MatchQualityEvaluator.CaseResult("duplicate", List.of("b"), List.of()))));
    }
}
