package pl.hubmalopolski.hub.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.hubmalopolski.hub.communication.IdeaCommunicationService;
import pl.hubmalopolski.hub.config.SecurityConfig;
import pl.hubmalopolski.hub.repo.AppUserRepository;
import pl.hubmalopolski.hub.repo.IdeaRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IdeaCommunicationController.class)
@Import(SecurityConfig.class)
class IdeaCommunicationApiTests {
    @Autowired MockMvc mvc;
    @MockitoBean IdeaCommunicationService communication;
    @MockitoBean IdeaRepository ideas;
    @MockitoBean AppUserRepository users;

    @Test
    void staffCanReplyToIdeaAndMemberCannot() throws Exception {
        when(communication.reply(eq(5L), eq("staff@example.org"), anyString()))
                .thenReturn(new IdeaCommunicationService.ReplyDto(8L, 5L,
                        "Odpowiedź Hubu", "Pracownik", null));
        String body = "{\"body\":\"Odpowiedź Hubu\"}";
        mvc.perform(post("/api/v1/staff/ideas/5/replies").with(user("member").roles("MEMBER"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/staff/ideas/5/replies").with(user("staff@example.org").roles("STAFF"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Odpowiedź Hubu"));
    }

    @Test
    void memberCanReadOwnNotificationsAndRepliesOnlyAfterLogin() throws Exception {
        when(communication.notifications("author@example.org")).thenReturn(List.of(
                new IdeaCommunicationService.NotificationDto(3L, 5L, "IDEA_REPLY", "Odpowiedź na pomysł",
                        null, false)));
        mvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/notifications").with(user("author@example.org").roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("IDEA_REPLY"));
    }
}
