package pl.hubmalopolski.hub.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.hubmalopolski.hub.config.SecurityConfig;
import pl.hubmalopolski.hub.grants.GrantService;
import pl.hubmalopolski.hub.repo.AppUserRepository;
import pl.hubmalopolski.hub.workflow.HubWorkflowDto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GrantController.class)
@Import(SecurityConfig.class)
class GrantApiTests {
    @Autowired MockMvc mvc;
    @MockitoBean GrantService grants;
    @MockitoBean AppUserRepository users;

    @Test
    void visitorsCanSeeOnlyOpenCalls() throws Exception {
        when(grants.openCalls()).thenReturn(List.of(new HubWorkflowDto(1L, "GRANT_CALL", null,
                "OPEN", "Nabór", Map.of("fields", List.of()), null,
                Instant.parse("2026-10-04T00:00:00Z"), Instant.parse("2026-10-04T00:00:00Z"))));
        mvc.perform(get("/api/v1/grant-calls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void memberSubmitsAnswersButGuestIsRejected() throws Exception {
        when(grants.submitApplication(eq(9L), eq("member@example.org"), any()))
                .thenReturn(new HubWorkflowDto(22L, "GRANT_APPLICATION", 9L, "SUBMITTED", "Nabór",
                        Map.of("answers", Map.of("problem", "Samotność")), null,
                        Instant.parse("2026-10-04T00:00:00Z"), Instant.parse("2026-10-04T00:00:00Z")));
        String body = "{\"answers\":{\"problem\":\"Samotność\"}}";
        mvc.perform(post("/api/v1/grant-calls/9/applications").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/grant-calls/9/applications")
                        .with(user("member@example.org").roles("MEMBER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payload.answers.problem").value("Samotność"));
    }

    @Test
    void onlyAdminCanConfigureGrantCall() throws Exception {
        mvc.perform(post("/api/v1/admin/grant-calls").with(user("member").roles("MEMBER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nabór\",\"opensAt\":\"2026-10-01T00:00:00Z\","
                                + "\"closesAt\":\"2026-11-01T00:00:00Z\",\"fields\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateVersionedGrantForm() throws Exception {
        when(grants.createCall(eq("admin@example.org"), eq("Nabór na innowacje"), any(), any(), any(),
                eq("OPEN"), any())).thenReturn(new HubWorkflowDto(9L, "GRANT_CALL", null, "OPEN",
                "Nabór na innowacje", Map.of("fields", List.of(Map.of("key", "problem"))), null,
                Instant.parse("2026-10-04T00:00:00Z"), Instant.parse("2026-10-04T00:00:00Z")));
        mvc.perform(post("/api/v1/admin/grant-calls").with(user("admin@example.org").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nabór na innowacje\",\"description\":\"Opis naboru\","
                                + "\"opensAt\":\"2026-10-01T00:00:00Z\",\"closesAt\":\"2026-11-01T00:00:00Z\","
                                + "\"status\":\"OPEN\",\"fields\":[{\"key\":\"problem\",\"label\":\"Jaki problem?\","
                                + "\"type\":\"TEXTAREA\",\"required\":true}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payload.fields[0].key").value("problem"));
    }

    @Test
    void invalidGrantFieldTypeIsRejectedByTheContract() throws Exception {
        mvc.perform(post("/api/v1/admin/grant-calls").with(user("admin@example.org").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nabór\",\"opensAt\":\"2026-10-01T00:00:00Z\","
                                + "\"closesAt\":\"2026-11-01T00:00:00Z\",\"fields\":[{\"key\":\"x\","
                                + "\"label\":\"Pytanie\",\"type\":\"FREE_TEXT\",\"required\":true}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void staffCanChangeApplicationStatus() throws Exception {
        when(grants.changeApplicationStatus(22L, "staff@example.org", "UNDER_REVIEW"))
                .thenReturn(new HubWorkflowDto(22L, "GRANT_APPLICATION", 9L, "UNDER_REVIEW", "Nabór",
                        Map.of(), "Autor", Instant.parse("2026-10-04T00:00:00Z"),
                        Instant.parse("2026-10-04T00:00:00Z")));
        mvc.perform(patch("/api/v1/staff/grant-applications/22/status")
                        .with(user("staff@example.org").roles("STAFF")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
    }
}
