package pl.hubmalopolski.hub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:hub-context;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.vectorstore.pgvector.enabled=false",
        "hub.seed-vectors=false",
        "HUB_STAFF_EMAIL=staff-bootstrap@example.org",
        "HUB_STAFF_PASSWORD=staff-test-password",
        "HUB_ADMIN_EMAIL=admin-bootstrap@example.org",
        "HUB_ADMIN_PASSWORD=admin-test-password"
})
@AutoConfigureMockMvc
class HubAppApplicationTests {
    @MockitoBean VectorStore vectorStore;
    @MockitoBean ChatModel chatModel;
    @Autowired MockMvc mvc;

    @Test
    void contextLoads() {
    }

    @Test
    void residentCanRegisterLoginAndReadOwnIdeas() throws Exception {
        mvc.perform(post("/api/v1/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"anna@example.org\",\"password\":\"very-long-password\",\"displayName\":\"Anna\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"));

        var login = mvc.perform(formLogin().user("anna@example.org").password("very-long-password"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(get("/api/v1/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("anna@example.org"))
                .andExpect(jsonPath("$.roles[0]").value("MEMBER"));
        mvc.perform(get("/api/v1/ideas?mine=true").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void configuredStaffAndAdminAccountsCanLogin() throws Exception {
        var staffLogin = mvc.perform(formLogin().user("staff-bootstrap@example.org")
                        .password("staff-test-password"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var staffSession = (MockHttpSession) staffLogin.getRequest().getSession(false);
        mvc.perform(get("/api/v1/me").session(staffSession))
                .andExpect(jsonPath("$.roles[0]").value("STAFF"));
        mvc.perform(get("/api/v1/reports").session(staffSession))
                .andExpect(status().isOk());

        var adminLogin = mvc.perform(formLogin().user("admin-bootstrap@example.org")
                        .password("admin-test-password"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var adminSession = (MockHttpSession) adminLogin.getRequest().getSession(false);
        mvc.perform(get("/api/v1/me").session(adminSession))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
        mvc.perform(get("/api/v1/admin/ideas").session(adminSession))
                .andExpect(status().isOk());
    }

    @Test
    void openApiIncludesNewEndpointsAndEnums() throws Exception {
        String json = new String(mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertTrue(json.contains("/api/v1/regions"));
        assertTrue(json.contains("/api/v1/matches/{reportId}"));
        assertTrue(json.contains("/api/v1/register"));
        assertTrue(json.contains("/api/v1/admin/trends"));
        assertTrue(json.contains("/api/v1/admin/resources"));
        assertTrue(json.contains("/api/v1/staff/ideas/{id}/replies"));
        assertTrue(json.contains("/api/v1/notifications"));
        assertTrue(json.contains("/api/v1/grant-calls"));
        assertTrue(json.contains("/api/v1/innovations/{id}/tester-feedback"));
        assertTrue(json.contains("/api/v1/mentor/conversations"));
        assertTrue(json.contains("/api/v1/middleman/plans"));
        assertTrue(json.contains("NEW_GRANT_APPLICATION"));
        assertTrue(json.contains("MALOPOLSKA"));

        String exportDir = System.getProperty("export.openapi.dir");
        if (exportDir != null) {
            Files.writeString(Path.of(exportDir, "openapi.json"), json);
            String yaml = new String(mvc.perform(get("/v3/api-docs.yaml"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray(),
                    StandardCharsets.UTF_8);
            Files.writeString(Path.of(exportDir, "openapi.yaml"), yaml);
        }
    }
}
