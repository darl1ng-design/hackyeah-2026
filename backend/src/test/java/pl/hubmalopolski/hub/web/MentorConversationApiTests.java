package pl.hubmalopolski.hub.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.hubmalopolski.hub.config.SecurityConfig;
import pl.hubmalopolski.hub.communication.MentorConversationService;
import pl.hubmalopolski.hub.repo.AppUserRepository;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MentorConversationController.class)
@Import(SecurityConfig.class)
class MentorConversationApiTests {
    @Autowired MockMvc mvc;
    @MockitoBean MentorConversationService conversations;
    @MockitoBean AppUserRepository users;

    @Test
    void guestCannotOpenMentorConversation() throws Exception {
        mvc.perform(post("/api/v1/mentor/conversations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Wsparcie\",\"body\":\"Potrzebuję konsultacji.\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberCanOpenConversationAndStaffCanReply() throws Exception {
        var first = new MentorConversationService.MessageDto(12L, "Anna", "MEMBER", "Potrzebuję konsultacji.",
                Instant.parse("2026-10-04T00:00:00Z"));
        var thread = new MentorConversationService.ConversationDto(8L, "Wsparcie", "OPEN", "Anna",
                Instant.parse("2026-10-04T00:00:00Z"), List.of(first));
        when(conversations.open("member@example.org", "Wsparcie", "Potrzebuję konsultacji.")).thenReturn(thread);
        when(conversations.reply(eq(8L), eq("staff@example.org"), eq("Chętnie porozmawiamy.")))
                .thenReturn(new MentorConversationService.ConversationDto(8L, "Wsparcie", "OPEN", "Anna",
                        Instant.parse("2026-10-04T00:00:00Z"), List.of(first,
                        new MentorConversationService.MessageDto(13L, "Ekspert", "STAFF", "Chętnie porozmawiamy.",
                                Instant.parse("2026-10-04T00:01:00Z")))));

        mvc.perform(post("/api/v1/mentor/conversations").with(user("member@example.org").roles("MEMBER"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Wsparcie\",\"body\":\"Potrzebuję konsultacji.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messages[0].authorRole").value("MEMBER"));
        mvc.perform(post("/api/v1/staff/mentor/conversations/8/messages")
                        .with(user("staff@example.org").roles("STAFF")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Chętnie porozmawiamy.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messages[1].body").value("Chętnie porozmawiamy."));
    }

    @Test
    void memberCanReadOwnConversationOnly() throws Exception {
        when(conversations.get(8L, "member@example.org")).thenReturn(new MentorConversationService.ConversationDto(
                8L, "Wsparcie", "OPEN", "Anna", Instant.parse("2026-10-04T00:00:00Z"), List.of()));
        when(conversations.get(8L, "other@example.org"))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        mvc.perform(get("/api/v1/mentor/conversations/8").with(user("member@example.org").roles("MEMBER")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/mentor/conversations/8").with(user("other@example.org").roles("MEMBER")))
                .andExpect(status().isNotFound());
    }
}
