package pl.hubmalopolski.hub.match;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** Opt-in only: requires the target PostgreSQL and model endpoints plus expert labels. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "HUB_MATCH_EVAL", matches = "true")
class LiveMatchQualityEvaluationTests {
    record EvaluationCase(String id, String problem, List<String> expectedInnovationIds) {}

    @Autowired MatchmakingService matchmaking;
    @Autowired ObjectMapper objectMapper;

    @Test
    void evaluatesTargetStackAgainstExpertLabels() throws Exception {
        String fileName = System.getenv("HUB_MATCH_EVAL_FILE");
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalStateException("HUB_MATCH_EVAL_FILE must point to an expert-labelled JSON file");
        }
        EvaluationCase[] cases = objectMapper.readValue(Files.readString(Path.of(fileName)), EvaluationCase[].class);
        assertFalse(cases.length == 0, "Evaluation file must contain expert-labelled examples");
        List<MatchQualityEvaluator.CaseResult> results = List.of(cases).stream().map(testCase ->
                new MatchQualityEvaluator.CaseResult(testCase.id(), testCase.expectedInnovationIds(),
                        matchmaking.match(testCase.problem()).stream()
                                .map(result -> String.valueOf(result.innovation().getId())).toList())).toList();
        MatchQualityEvaluator.Metrics metrics = MatchQualityEvaluator.evaluate(results);
        System.out.printf("Match benchmark: cases=%d, hitCases=%d, Recall@5=%.4f, MRR@5=%.4f, missed=%s%n",
                metrics.cases(), metrics.casesWithHits(), metrics.recallAtFive(), metrics.mrrAtFive(),
                metrics.missedCaseIds());
    }
}
