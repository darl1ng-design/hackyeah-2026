package pl.hubmalopolski.hub.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.hubmalopolski.hub.config.SecurityConfig;
import pl.hubmalopolski.hub.repo.AppUserRepository;
import pl.hubmalopolski.hub.trends.TrendService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminTrendsController.class)
@Import(SecurityConfig.class)
class AdminTrendsApiTests {
    @Autowired MockMvc mvc;
    @MockitoBean TrendService trends;
    @MockitoBean AppUserRepository users;

    @Test
    void onlyAdminCanReadAggregatedNeeds() throws Exception {
        when(trends.get(any(Instant.class), any(Instant.class))).thenReturn(
                new TrendService.TrendsDto(3,
                        List.of(new TrendService.CountDto("1", "Samotność", 3)),
                        List.of(), List.of(), List.of(new TrendService.MonthCountDto("2026-10", 3))));

        mvc.perform(get("/api/v1/admin/trends")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/trends").with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/trends").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.byArea[0].label").value("Samotność"))
                .andExpect(jsonPath("$.byMonth[0].month").value("2026-10"));
    }

    @Test
    void invalidDateRangeIsRejected() throws Exception {
        mvc.perform(get("/api/v1/admin/trends?from=2026-10-05&to=2026-10-04")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }
}
