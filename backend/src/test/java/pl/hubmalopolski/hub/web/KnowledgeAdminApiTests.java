package pl.hubmalopolski.hub.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.hubmalopolski.hub.catalog.KnowledgeService;
import pl.hubmalopolski.hub.config.SecurityConfig;
import pl.hubmalopolski.hub.domain.Innovation;
import pl.hubmalopolski.hub.domain.InnovationStatus;
import pl.hubmalopolski.hub.domain.Region;
import pl.hubmalopolski.hub.repo.AppUserRepository;
import pl.hubmalopolski.hub.repo.InnovationRepository;
import pl.hubmalopolski.hub.repo.ResourceRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KnowledgeAdminController.class)
@Import(SecurityConfig.class)
class KnowledgeAdminApiTests {
    @Autowired MockMvc mvc;
    @MockitoBean KnowledgeService knowledge;
    @MockitoBean AppUserRepository users;
    @MockitoBean InnovationRepository innovations;
    @MockitoBean ResourceRepository resources;

    @Test
    void adminCanEditAndUnpublishInnovation() throws Exception {
        Innovation innovation = new Innovation("Poprawiona nazwa", "Opis", null, null,
                InnovationStatus.TESTOWANA, Region.MALOPOLSKA, null);
        innovation.setPublished(false);
        when(knowledge.updateInnovation(eq(4L), any())).thenReturn(innovation);
        String body = "{\"title\":\"Poprawiona nazwa\",\"status\":\"TESTOWANA\","
                + "\"region\":\"MALOPOLSKA\",\"published\":false}";

        mvc.perform(put("/api/v1/admin/innovations/4").with(user("member").roles("MEMBER"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/admin/innovations/4").with(user("admin").roles("ADMIN"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Poprawiona nazwa"))
                .andExpect(jsonPath("$.published").value(false));
    }

    @Test
    void knowledgeEditRequiresCsrfAndValidTitle() throws Exception {
        mvc.perform(put("/api/v1/admin/innovations/4").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nowa\",\"status\":\"ROZWOJ\",\"published\":true}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/admin/innovations/4").with(user("admin").roles("ADMIN"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"status\":\"ROZWOJ\",\"published\":true}"))
                .andExpect(status().isBadRequest());
    }
}
