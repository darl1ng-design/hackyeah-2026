package pl.hubmalopolski.hub.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pl.hubmalopolski.hub.trends.TrendService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/admin/trends")
@SecurityRequirement(name = "cookieAuth")
public class AdminTrendsController {
    private static final LocalDate EARLIEST = LocalDate.of(1900, 1, 1);
    private static final LocalDate LATEST = LocalDate.of(9999, 12, 31);
    private final TrendService trends;

    public AdminTrendsController(TrendService trends) {
        this.trends = trends;
    }

    @GetMapping
    public TrendService.TrendsDto get(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate start = from == null ? EARLIEST : from;
        LocalDate end = to == null ? LATEST : to;
        if (start.isBefore(EARLIEST) || end.isAfter(LATEST) || start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date range must be ordered and within years 1900..9999");
        }
        Instant startInstant = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endExclusive = end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return trends.get(startInstant, endExclusive);
    }
}
