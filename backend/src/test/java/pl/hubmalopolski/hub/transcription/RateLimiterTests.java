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

    @Test
    void memoryStaysBoundedUnderKeyFlood() {
        var limiter = new RateLimiter(5);
        for (int i = 0; i <= RateLimiter.MAX_KEYS + 1; i++) limiter.tryAcquire("ip" + i, 0);
        assertThat(limiter.tryAcquire("fresh", 1_000)).isFalse(); // full this minute
        assertThat(limiter.tryAcquire("fresh", 60_000)).isTrue();  // stale windows pruned
    }
}
