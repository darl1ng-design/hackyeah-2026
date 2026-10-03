package pl.hubmalopolski.hub.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pl.hubmalopolski.hub.ai.IdeaAssistant;
import pl.hubmalopolski.hub.ai.ProblemClassifier;
import pl.hubmalopolski.hub.config.SecurityConfig;
import pl.hubmalopolski.hub.match.MatchmakingService;
import pl.hubmalopolski.hub.match.MatchReportService;
import pl.hubmalopolski.hub.repo.ChallengeAreaRepository;
import pl.hubmalopolski.hub.repo.AppUserRepository;
import pl.hubmalopolski.hub.repo.IdeaRepository;
import pl.hubmalopolski.hub.repo.InnovationRepository;
import pl.hubmalopolski.hub.repo.ProblemReportRepository;
import pl.hubmalopolski.hub.repo.ResourceRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiController.class)
@Import(SecurityConfig.class)
class ApiSecurityTests {
    @Autowired MockMvc mvc;
    @MockitoBean InnovationRepository innovations;
    @MockitoBean ChallengeAreaRepository areas;
    @MockitoBean ResourceRepository resources;
    @MockitoBean IdeaRepository ideas;
    @MockitoBean ProblemReportRepository reports;
    @MockitoBean MatchmakingService matchmaking;
    @MockitoBean MatchReportService matchReports;
    @MockitoBean ProblemClassifier classifier;
    @MockitoBean IdeaAssistant assistant;
    @MockitoBean AppUserRepository users;

    @BeforeEach
    void stubIdeaSave() {
        when(ideas.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void anonymousUserGets401ForSessionDetails() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserGetsIdentityAndRoles() throws Exception {
        mvc.perform(get("/api/v1/me").with(user("alice").roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.roles[0]").value("MEMBER"));
    }

    @Test
    void anonymousUserCanCreateIdeaWithCsrf() throws Exception {
        mvc.perform(post("/api/v1/ideas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Pomysł\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void publicMutationRequiresCsrfToken() throws Exception {
        mvc.perform(post("/api/v1/ideas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Pomysł\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserCannotReadStaffReports() throws Exception {
        mvc.perform(get("/api/v1/reports"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberCannotReadStaffReportsOrModerateIdeas() throws Exception {
        mvc.perform(get("/api/v1/reports").with(user("member@example.org").roles("MEMBER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/ideas").with(user("member@example.org").roles("MEMBER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCanReadReports() throws Exception {
        mvc.perform(get("/api/v1/reports").with(user("staff@example.org").roles("STAFF")))
                .andExpect(status().isOk());
    }

    @Test
    void csrfTokenIsAvailableForBrowserClient() throws Exception {
        mvc.perform(get("/api/v1/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
    }
}
