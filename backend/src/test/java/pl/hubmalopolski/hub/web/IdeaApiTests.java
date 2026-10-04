package pl.hubmalopolski.hub.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.hubmalopolski.hub.ai.IdeaAssistant;
import pl.hubmalopolski.hub.ai.ProblemClassifier;
import pl.hubmalopolski.hub.config.SecurityConfig;
import pl.hubmalopolski.hub.communication.IdeaCommunicationService;
import pl.hubmalopolski.hub.domain.AppUser;
import pl.hubmalopolski.hub.domain.AppUserRole;
import pl.hubmalopolski.hub.domain.Idea;
import pl.hubmalopolski.hub.domain.IdeaModerationStatus;
import pl.hubmalopolski.hub.domain.IdeaStage;
import pl.hubmalopolski.hub.match.MatchReportService;
import pl.hubmalopolski.hub.match.MatchmakingService;
import pl.hubmalopolski.hub.repo.AppUserRepository;
import pl.hubmalopolski.hub.repo.ChallengeAreaRepository;
import pl.hubmalopolski.hub.repo.IdeaRepository;
import pl.hubmalopolski.hub.repo.InnovationRepository;
import pl.hubmalopolski.hub.repo.ProblemReportRepository;
import pl.hubmalopolski.hub.repo.ResourceRepository;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiController.class)
@Import(SecurityConfig.class)
class IdeaApiTests {
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
    @MockitoBean IdeaCommunicationService communication;

    @BeforeEach
    void saveReturnsIdea() {
        when(ideas.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void accountCanCreatePendingIdeaWithOwnerName() throws Exception {
        AppUser owner = new AppUser("jan@example.org", "hash", "Jan", AppUserRole.MEMBER);
        when(users.findByEmail("jan@example.org")).thenReturn(Optional.of(owner));
        mvc.perform(post("/api/v1/ideas").with(user("jan@example.org").roles("MEMBER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Pomysł\",\"stage\":\"MYSL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("Jan"))
                .andExpect(jsonPath("$.moderationStatus").value("PENDING"));
    }

    @Test
    void mineRequiresAccount() throws Exception {
        mvc.perform(get("/api/v1/ideas?mine=true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accountCanSeeOwnPendingIdea() throws Exception {
        AppUser owner = new AppUser("anna@example.org", "hash", "Anna", AppUserRole.MEMBER);
        Idea idea = new Idea("Pomysł", null, null, IdeaStage.MYSL, null, "Anna", owner);
        when(ideas.findByOwnerEmailOrderByCreatedAtDesc("anna@example.org")).thenReturn(List.of(idea));

        mvc.perform(get("/api/v1/ideas?mine=true")
                        .with(user("anna@example.org").roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].moderationStatus").value("PENDING"));
    }

    @Test
    void pendingIdeaIsPrivateUntilApproved() throws Exception {
        Idea idea = new Idea("Pomysł", null, null, IdeaStage.MYSL, null, "Jan", null);
        when(ideas.findById(42L)).thenReturn(Optional.of(idea));

        mvc.perform(get("/api/v1/ideas/42"))
                .andExpect(status().isNotFound());

        idea.setModerationStatus(IdeaModerationStatus.APPROVED);
        mvc.perform(get("/api/v1/ideas/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Pomysł"));
    }

    @Test
    void adminCanApproveIdea() throws Exception {
        Idea idea = new Idea("Pomysł", null, null, IdeaStage.MYSL, null, "Jan", null);
        when(ideas.findById(42L)).thenReturn(Optional.of(idea));

        mvc.perform(patch("/api/v1/admin/ideas/42/moderation")
                        .with(user("admin@example.org").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("APPROVED"));
    }
}
