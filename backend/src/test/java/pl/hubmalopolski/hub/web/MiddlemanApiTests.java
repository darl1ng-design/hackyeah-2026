package pl.hubmalopolski.hub.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.hubmalopolski.hub.config.SecurityConfig;
import pl.hubmalopolski.hub.middleman.MiddlemanService;
import pl.hubmalopolski.hub.repo.AppUserRepository;
import pl.hubmalopolski.hub.workflow.HubWorkflowDto;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MiddlemanController.class)
@Import(SecurityConfig.class)
class MiddlemanApiTests {
    @Autowired MockMvc mvc;
    @MockitoBean MiddlemanService middleman;
    @MockitoBean AppUserRepository users;
    @MockitoBean pl.hubmalopolski.hub.ai.IdeaStoryParser storyParser;

    @Test
    void guestCannotParseDictation() throws Exception {
        mvc.perform(post("/api/v1/middleman/parse").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"GOPS Limanowa\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberGetsDictationSplitIntoAdaptationFields() throws Exception {
        when(storyParser.parseAdaptation(anyString())).thenReturn(new pl.hubmalopolski.hub.ai.IdeaStoryParser
                .ParsedAdaptation("Klub sąsiedzki", "GOPS Limanowa", "Seniorzy", "Samotność.", "Mały budżet."));
        mvc.perform(post("/api/v1/middleman/parse").with(user("member@example.org").roles("MEMBER"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Chcemy klub sąsiedzki w GOPS Limanowa.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institution").value("GOPS Limanowa"))
                .andExpect(jsonPath("$.innovation").value("Klub sąsiedzki"));
    }

    @Test
    void parseReturns503WhenModelIsDown() throws Exception {
        when(storyParser.parseAdaptation(anyString())).thenThrow(new IllegalStateException("down"));
        mvc.perform(post("/api/v1/middleman/parse").with(user("member@example.org").roles("MEMBER"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"x\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void guestCannotGeneratePlan() throws Exception {
        mvc.perform(post("/api/v1/middleman/plans").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"innovationId\":2,\"institution\":\"CUS\",\"targetGroup\":\"Seniorzy\","
                                + "\"need\":\"Usługi lokalne\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberCanGenerateAndReopenSavedAiDraft() throws Exception {
        var draft = new HubWorkflowDto(31L, "MIDDLEMAN_PLAN", 2L, "AI_DRAFT", "Usługa wsparcia",
                Map.of("draft", true, "plan", Map.of("steps", java.util.List.of("Uruchom punkt konsultacyjny"))),
                null, Instant.parse("2026-10-04T00:00:00Z"), Instant.parse("2026-10-04T00:00:00Z"));
        when(middleman.generate(eq("member@example.org"), eq(2L), eq("CUS"), eq("Seniorzy"),
                eq("Usługi lokalne"), eq("Dostępność budynku"))).thenReturn(draft);
        when(middleman.get(31L, "member@example.org")).thenReturn(draft);
        mvc.perform(post("/api/v1/middleman/plans").with(user("member@example.org").roles("MEMBER"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"innovationId\":2,\"institution\":\"CUS\",\"targetGroup\":\"Seniorzy\","
                                + "\"need\":\"Usługi lokalne\",\"constraints\":\"Dostępność budynku\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AI_DRAFT"))
                .andExpect(jsonPath("$.payload.draft").value(true));
        mvc.perform(get("/api/v1/middleman/plans/31").with(user("member@example.org").roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.plan.steps[0]").value("Uruchom punkt konsultacyjny"));
    }
}
