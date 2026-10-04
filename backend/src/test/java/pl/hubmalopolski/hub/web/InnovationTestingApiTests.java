package pl.hubmalopolski.hub.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.hubmalopolski.hub.config.SecurityConfig;
import pl.hubmalopolski.hub.testing.InnovationTestingService;
import pl.hubmalopolski.hub.workflow.HubWorkflowDto;
import pl.hubmalopolski.hub.repo.AppUserRepository;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InnovationTestingController.class)
@Import(SecurityConfig.class)
class InnovationTestingApiTests {
    @Autowired MockMvc mvc;
    @MockitoBean InnovationTestingService testing;
    @MockitoBean AppUserRepository users;

    @Test
    void guestCannotSendTesterFeedback() throws Exception {
        mvc.perform(post("/api/v1/innovations/7/tester-feedback").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interested\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberCanExpressInterestAndSeeOwnFeedback() throws Exception {
        var dto = new HubWorkflowDto(51L, "TESTER_FEEDBACK", 7L, "SUBMITTED", "Innowacja",
                Map.of("interested", true), null, Instant.parse("2026-10-04T00:00:00Z"),
                Instant.parse("2026-10-04T00:00:00Z"));
        when(testing.submit(7L, "member@example.org", true, 5, "Działa dobrze", "Więcej terminów"))
                .thenReturn(dto);
        when(testing.mine(7L, "member@example.org")).thenReturn(dto);

        mvc.perform(post("/api/v1/innovations/7/tester-feedback")
                        .with(user("member@example.org").roles("MEMBER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interested\":true,\"rating\":5,\"feedback\":\"Działa dobrze\",\"suggestion\":\"Więcej terminów\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
        mvc.perform(get("/api/v1/innovations/7/tester-feedback/mine")
                        .with(user("member@example.org").roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.interested").value(true));
    }

    @Test
    void feedbackRequiresExplicitInterestAndValidRating() throws Exception {
        mvc.perform(post("/api/v1/innovations/7/tester-feedback")
                        .with(user("member@example.org").roles("MEMBER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interested\":false,\"rating\":6}"))
                .andExpect(status().isBadRequest());
    }
}
