package pl.hubmalopolski.hub.transcription;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTests {
    @Test
    void allowsLimitPerMinuteThenResetsNextMinute() {
        var limiter = new RateLimiter(2);
        assertThat(limiter.tryAcquire("a", 0)).isTrue();
        assertThat(limiter.tryAcquire("a", 1_000)).isTrue();
        assertThat(limiter.tryAcquire("a", 2_000)).isFalse();
        assertThat(limiter.tryAcquire("b", 2_000)).isTrue();
        assertThat(limiter.tryAcquire("a", 60_000)).isTrue();
    }
}
