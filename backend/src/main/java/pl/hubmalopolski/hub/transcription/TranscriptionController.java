package pl.hubmalopolski.hub.transcription;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/transcribe")
public class TranscriptionController {
    public record TranscriptionDto(String text) {}

    static final long MAX_BYTES = 10 * 1024 * 1024; // ~10 min of 128 kbps opus; dictation is far shorter
    private static final Set<String> LANGUAGES = Set.of("pl", "en", "uk");
    private static final Map<String, String> EXT = Map.of(
            "audio/webm", "webm", "audio/ogg", "ogg", "audio/mp4", "m4a",
            "audio/mpeg", "mp3", "audio/wav", "wav", "audio/x-wav", "wav");
    private static final Logger log = LoggerFactory.getLogger(TranscriptionController.class);

    private final TranscriptionService transcription;
    private final RateLimiter limiter;
    private final RateLimiter global;

    // Public endpoint (guests dictate on the match form) spends GPU time: per-IP limit plus a
    // global cap so rotating IPs can't exceed it; the LiteLLM key's own rpm limit backs both.
    public TranscriptionController(TranscriptionService transcription,
                                   @Value("${hub.transcription.per-minute:10}") int perMinute,
                                   @Value("${hub.transcription.global-per-minute:30}") int globalPerMinute) {
        this.transcription = transcription;
        this.limiter = new RateLimiter(perMinute);
        this.global = new RateLimiter(globalPerMinute);
    }

    @GetMapping("/health")
    public Map<String, Boolean> health() {
        return Map.of("available", transcription.available());
    }

    @PostMapping(consumes = "multipart/form-data")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public TranscriptionDto transcribe(@RequestParam("file") MultipartFile file,
                                       @RequestParam(defaultValue = "pl") String language,
                                       HttpServletRequest request) throws Exception {
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nagranie jest puste.");
        if (file.getSize() > MAX_BYTES)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Nagranie jest za długie.");
        if (!LANGUAGES.contains(language))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieobsługiwany język.");
        String type = file.getContentType() == null ? "" : file.getContentType().split(";")[0].trim();
        String ext = EXT.get(type);
        if (ext == null)
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Nieobsługiwany format nagrania.");
        long now = System.currentTimeMillis();
        if (!limiter.tryAcquire(request.getRemoteAddr(), now) || !global.tryAcquire("*", now))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Za dużo nagrań. Spróbuj za minutę.");
        if (!transcription.available())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Rozpoznawanie mowy jest niedostępne.");
        try {
            return new TranscriptionDto(transcription.transcribe(file.getBytes(), "audio." + ext, language));
        } catch (RuntimeException e) {
            log.warn("Transcription failed: {}", e.toString());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Nie udało się rozpoznać nagrania. Spróbuj ponownie za chwilę.");
        }
    }

    /** Our reasons are user-facing Polish copy; Spring's default error body drops them. */
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", String.valueOf(e.getReason())));
    }
}
