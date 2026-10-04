package pl.hubmalopolski.hub.match;

import java.util.List;

/**
 * Structured output z LLM (rerank + uzasadnienie).
 */
public record MatchExplanations(List<MatchExplanation> matches) {
    public record MatchExplanation(long innovationId, String why) {
    }
}
