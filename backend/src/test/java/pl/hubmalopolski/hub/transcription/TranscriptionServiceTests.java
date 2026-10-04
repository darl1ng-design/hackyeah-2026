package pl.hubmalopolski.hub.transcription;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptionServiceTests {
    @Test
    void unwrapsJsonBodyAndKeepsPlainText() {
        assertThat(TranscriptionService.textOf("{\"text\":\" Dzień dobry \",\"usage\":null}")).isEqualTo("Dzień dobry");
        assertThat(TranscriptionService.textOf("  Dzień dobry\n")).isEqualTo("Dzień dobry");
        assertThat(TranscriptionService.textOf("{\"usage\":null}")).isEmpty();
    }
}
