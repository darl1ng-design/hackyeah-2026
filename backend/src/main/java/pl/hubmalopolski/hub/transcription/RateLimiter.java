package pl.hubmalopolski.hub.transcription;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed one-minute window per key.
 * ponytail: in-memory, single instance; move to Redis/bucket4j if the app is scaled out.
 */
class RateLimiter {
    private record Window(long minute, int count) {}

    static final int MAX_KEYS = 10_000;

    private final int perMinute;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    RateLimiter(int perMinute) {
        this.perMinute = perMinute;
    }

    boolean tryAcquire(String key, long nowMillis) {
        long minute = nowMillis / 60_000;
        if (windows.size() > MAX_KEYS) {
            windows.values().removeIf(w -> w.minute() < minute); // drop stale windows
            // still full within this minute (e.g. rotating IPv6 addresses): fail closed for new keys
            if (windows.size() > MAX_KEYS && !windows.containsKey(key)) return false;
        }
        Window w = windows.merge(key, new Window(minute, 1),
                (old, fresh) -> old.minute() == minute ? new Window(minute, old.count() + 1) : fresh);
        return w.count() <= perMinute;
    }
}
