package pl.hubmalopolski.hub.transcription;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed one-minute window per key.
 * ponytail: in-memory, single instance; move to Redis/bucket4j if the app is scaled out.
 */
class RateLimiter {
    private record Window(long minute, int count) {}

    private final int perMinute;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    RateLimiter(int perMinute) {
        this.perMinute = perMinute;
    }

    boolean tryAcquire(String key, long nowMillis) {
        long minute = nowMillis / 60_000;
        if (windows.size() > 10_000) windows.values().removeIf(w -> w.minute() < minute); // bound memory
        Window w = windows.merge(key, new Window(minute, 1),
                (old, fresh) -> old.minute() == minute ? new Window(minute, old.count() + 1) : fresh);
        return w.count() <= perMinute;
    }
}
