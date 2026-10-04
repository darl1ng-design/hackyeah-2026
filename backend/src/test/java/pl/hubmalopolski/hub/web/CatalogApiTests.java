package pl.hubmalopolski.hub.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pl.hubmalopolski.hub.ai.IdeaAssistant;
import pl.hubmalopolski.hub.ai.ProblemClassifier;
import pl.hubmalopolski.hub.config.SecurityConfig;
import pl.hubmalopolski.hub.communication.IdeaCommunicationService;
import pl.hubmalopolski.hub.domain.Innovation;
import pl.hubmalopolski.hub.domain.InnovationStatus;
import pl.hubmalopolski.hub.domain.Region;
import pl.hubmalopolski.hub.domain.ProblemReport;
import pl.hubmalopolski.hub.match.MatchmakingService;
import pl.hubmalopolski.hub.match.MatchReportService;
import pl.hubmalopolski.hub.repo.ChallengeAreaRepository;
import pl.hubmalopolski.hub.repo.AppUserRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiController.class)
@Import(SecurityConfig.class)
class CatalogApiTests {
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

    @Test
    void innovationsHavePageMetadata() throws Exception {
        when(innovations.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(invocation -> {
            Pageable page = invocation.getArgument(1);
            return new PageImpl<>(List.of(new Innovation("Pierwsza", null, null, null,
                    InnovationStatus.ROZWOJ, Region.MALOPOLSKA, null)), page, 2);
        });

        mvc.perform(get("/api/v1/innovations?page=0&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void innovationHasCreatedAt() throws Exception {
        when(innovations.findById(1L)).thenReturn(Optional.of(
                new Innovation("Pierwsza", null, null, null,
                        InnovationStatus.ROZWOJ, Region.MALOPOLSKA, null)));

        mvc.perform(get("/api/v1/innovations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void invalidPageSizeIsRejected() throws Exception {
        mvc.perform(get("/api/v1/innovations?size=0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidSortFieldIsRejected() throws Exception {
        mvc.perform(get("/api/v1/innovations?sort=unknown,desc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void regionDictionaryIsAvailable() throws Exception {
        mvc.perform(get("/api/v1/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").isNotEmpty());
    }

    @Test
    void legacyRegionSpellingRemainsAccepted() throws Exception {
        when(innovations.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        mvc.perform(get("/api/v1/innovations?region=malopolska"))
                .andExpect(status().isOk());
    }

    @Test
    void invalidIdeaStageIsRejected() throws Exception {
        when(ideas.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(post("/api/v1/ideas").with(user("member").roles("MEMBER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Pomysł\",\"stage\":\"NIEZNANY\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void savedMatchReportHasShareableAddress() throws Exception {
        when(matchReports.find(5L)).thenReturn(Optional.of(new MatchReportService.SavedReport(
                new ProblemReport("Opis problemu", Region.MALOPOLSKA, "Anonim"), List.of())));
        mvc.perform(get("/api/v1/matches/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches.length()").value(0));
    }

    @Test
    void guestCanSubmitMatchWithLegacyRegionSpelling() throws Exception {
        when(areas.findAll()).thenReturn(List.of());
        when(matchmaking.match(any())).thenReturn(List.of());
        when(matchReports.save(any(), any())).thenAnswer(invocation ->
                new MatchReportService.SavedReport(invocation.getArgument(0), List.of()));

        mvc.perform(post("/api/v1/matches").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Potrzebuję pomocy\",\"region\":\"malopolska\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportStatus").value("NOWE"));
    }
}
