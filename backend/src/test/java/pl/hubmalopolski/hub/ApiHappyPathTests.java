package pl.hubmalopolski.hub;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.hubmalopolski.hub.ai.IdeaAssistant;
import pl.hubmalopolski.hub.ai.ProblemClassifier;
import pl.hubmalopolski.hub.domain.AppUser;
import pl.hubmalopolski.hub.domain.AppUserRole;
import pl.hubmalopolski.hub.domain.ChallengeArea;
import pl.hubmalopolski.hub.domain.Innovation;
import pl.hubmalopolski.hub.domain.InnovationStatus;
import pl.hubmalopolski.hub.domain.Region;
import pl.hubmalopolski.hub.repo.AppUserRepository;
import pl.hubmalopolski.hub.repo.ChallengeAreaRepository;
import pl.hubmalopolski.hub.repo.InnovationRepository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:hub-happy-paths;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.vectorstore.pgvector.enabled=false",
        "hub.seed-vectors=false"
})
@AutoConfigureMockMvc
class ApiHappyPathTests {
    private static final String PASSWORD = "long-test-password";

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired InnovationRepository innovations;
    @Autowired ChallengeAreaRepository areas;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean VectorStore vectorStore;
    @MockitoBean ChatModel chatModel;
    @MockitoBean ProblemClassifier classifier;
    @MockitoBean IdeaAssistant assistant;

    @Test
    void guestCanReopenMatchWhileStaffAndAdminProcessReport() throws Exception {
        ChallengeArea area = areas.save(new ChallengeArea("Samotność", "Wsparcie osób samotnych"));
        innovations.save(new Innovation("Telefon sąsiedzki",
                "Wsparcie samotnych seniorów", "Telefon i wolontariusz", "Seniorzy",
                InnovationStatus.WDROZONA, Region.MALOPOLSKA, null));
        when(classifier.classify(anyString(), any())).thenReturn(area);

        SessionCsrf guest = csrf();
        MvcResult submitted = mvc.perform(post("/api/v1/matches").session(guest.session())
                        .header("X-CSRF-TOKEN", guest.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Samotny senior potrzebuje kontaktu\",\"region\":\"MALOPOLSKA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportStatus").value("NOWE"))
                .andExpect(jsonPath("$.matches[0].innovation.title").value("Telefon sąsiedzki"))
                .andReturn();
        long reportId = ((Number) JsonPath.read(submitted.getResponse().getContentAsString(), "$.reportId"))
                .longValue();

        mvc.perform(get("/api/v1/matches/{id}", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches[0].why").isNotEmpty());

        MockHttpSession staff = login("staff-happy@example.org", AppUserRole.STAFF);
        mvc.perform(get("/api/v1/reports").session(staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + reportId + ")].status").value("NOWE"));

        MockHttpSession admin = login("admin-report-happy@example.org", AppUserRole.ADMIN);
        SessionCsrf adminCsrf = csrf(admin);
        mvc.perform(patch("/api/v1/admin/reports/{id}/status", reportId)
                        .session(adminCsrf.session()).header("X-CSRF-TOKEN", adminCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"W_REALIZACJI\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("W_REALIZACJI"));
        mvc.perform(get("/api/v1/matches/{id}", reportId))
                .andExpect(jsonPath("$.reportStatus").value("W_REALIZACJI"));
    }

    @Test
    void memberIdeaMovesFromPrivatePendingToPublicApproved() throws Exception {
        SessionCsrf guest = csrf();
        mvc.perform(post("/api/v1/register").session(guest.session())
                        .header("X-CSRF-TOKEN", guest.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"idea-happy@example.org\",\"password\":\"" + PASSWORD
                                + "\",\"displayName\":\"Anna\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"));
        MockHttpSession member = loginExisting("idea-happy@example.org", PASSWORD);
        mvc.perform(get("/api/v1/me").session(member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("MEMBER"));

        SessionCsrf memberCsrf = csrf(member);
        MvcResult created = mvc.perform(post("/api/v1/ideas").session(memberCsrf.session())
                        .header("X-CSRF-TOKEN", memberCsrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Telefon sąsiedzki\",\"stage\":\"MYSL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("Anna"))
                .andExpect(jsonPath("$.moderationStatus").value("PENDING"))
                .andReturn();
        long ideaId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id"))
                .longValue();
        mvc.perform(get("/api/v1/ideas?mine=true").session(member))
                .andExpect(jsonPath("$[?(@.id == " + ideaId + ")].moderationStatus").value("PENDING"));
        mvc.perform(get("/api/v1/ideas/{id}", ideaId))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/ideas/{id}", ideaId).session(member))
                .andExpect(status().isOk());

        MockHttpSession admin = login("admin-idea-happy@example.org", AppUserRole.ADMIN);
        mvc.perform(get("/api/v1/admin/ideas").session(admin))
                .andExpect(jsonPath("$[?(@.id == " + ideaId + ")].title").value("Telefon sąsiedzki"));
        SessionCsrf adminCsrf = csrf(admin);
        mvc.perform(patch("/api/v1/admin/ideas/{id}/moderation", ideaId)
                        .session(adminCsrf.session()).header("X-CSRF-TOKEN", adminCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/ideas/{id}", ideaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("APPROVED"));
        mvc.perform(get("/api/v1/ideas"))
                .andExpect(jsonPath("$[?(@.id == " + ideaId + ")].author").value("Anna"));

        SessionCsrf logoutCsrf = csrf(member);
        mvc.perform(post("/logout").session(logoutCsrf.session())
                        .header("X-CSRF-TOKEN", logoutCsrf.token()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/v1/me").session(member))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void guestCanAskForHelpButCannotSubmitIdeaWithoutAccount() throws Exception {
        SessionCsrf guest = csrf();
        mvc.perform(post("/api/v1/ideas").session(guest.session())
                        .header("X-CSRF-TOKEN", guest.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Ławka sąsiedzka\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/ideas/assistant").session(guest.session())
                        .header("X-CSRF-TOKEN", guest.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Jak zbudować ławkę?\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanAddInnovationAndPublicCanFilterKnowledgeResources() throws Exception {
        ChallengeArea area = areas.save(new ChallengeArea("Wykluczenie cyfrowe", "Wsparcie cyfrowe"));
        jdbc.update("insert into resource (name, url, kind, published) values (?, ?, ?, true)",
                "Canvas innowacji", "https://example.org/canvas", "CANVAS");
        MockHttpSession admin = login("admin-catalog-happy@example.org", AppUserRole.ADMIN);
        SessionCsrf adminCsrf = csrf(admin);
        MvcResult created = mvc.perform(post("/api/v1/admin/innovations")
                        .session(adminCsrf.session()).header("X-CSRF-TOKEN", adminCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Cyfrowy sąsiad\",\"summary\":\"Nauka telefonu\","
                                + "\"region\":\"MALOPOLSKA\",\"status\":\"ROZWOJ\",\"areaId\":"
                                + area.getId() + ",\"published\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.area.name").value("Wykluczenie cyfrowe"))
                .andReturn();
        long innovationId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id"))
                .longValue();

        mvc.perform(get("/api/v1/innovations").param("region", "MALOPOLSKA")
                        .param("areaId", area.getId().toString()).param("q", "Cyfrowy")
                        .param("page", "0").param("size", "1").param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Cyfrowy sąsiad"));
        mvc.perform(get("/api/v1/innovations/{id}", innovationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
        mvc.perform(get("/api/v1/areas"))
                .andExpect(jsonPath("$[?(@.name == 'Wykluczenie cyfrowe')].id").isNotEmpty());
        mvc.perform(get("/api/v1/regions"))
                .andExpect(jsonPath("$[?(@.code == 'MALOPOLSKA')].label").value("Małopolska"));
        mvc.perform(get("/api/v1/resources"))
                .andExpect(jsonPath("$[?(@.name == 'Canvas innowacji')].kind").value("CANVAS"));
    }

    @Test
    void guestCanAskAssistantWithConversationContext() throws Exception {
        when(assistant.coach(anyString(), any(), any())).thenReturn("Zapytaj seniorów o potrzeby.");
        SessionCsrf guest = csrf();
        mvc.perform(post("/api/v1/ideas/assistant").session(guest.session())
                        .header("X-CSRF-TOKEN", guest.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Co dalej?\",\"history\":[{\"role\":\"USER\","
                                + "\"content\":\"Mam pomysł\"},{\"role\":\"ASSISTANT\","
                                + "\"content\":\"Opisz odbiorców\"}],\"ideaContext\":{\"title\":\"Telefon\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Zapytaj seniorów o potrzeby."));
    }

    @Test
    void adminCanEditKnowledgeAndControlPublicVisibility() throws Exception {
        MockHttpSession admin = login("admin-knowledge-happy@example.org", AppUserRole.ADMIN);
        SessionCsrf token = csrf(admin);
        MvcResult areaResult = mvc.perform(post("/api/v1/admin/areas").session(token.session())
                        .header("X-CSRF-TOKEN", token.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Zdrowie psychiczne\",\"description\":\"Wsparcie\"}"))
                .andExpect(status().isCreated()).andReturn();
        long areaId = ((Number) JsonPath.read(areaResult.getResponse().getContentAsString(), "$.id")).longValue();
        mvc.perform(put("/api/v1/admin/areas/{id}", areaId).session(token.session())
                        .header("X-CSRF-TOKEN", token.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dobrostan psychiczny\",\"description\":\"Nowy opis\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dobrostan psychiczny"));

        MvcResult resourceResult = mvc.perform(post("/api/v1/admin/resources").session(token.session())
                        .header("X-CSRF-TOKEN", token.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Poradnik testowy\",\"url\":\"https://example.org/one\","
                                + "\"kind\":\"PUBLIKACJE\",\"published\":false}"))
                .andExpect(status().isCreated()).andReturn();
        long resourceId = ((Number) JsonPath.read(resourceResult.getResponse().getContentAsString(), "$.id"))
                .longValue();
        mvc.perform(get("/api/v1/resources"))
                .andExpect(jsonPath("$[?(@.id == " + resourceId + ")]").isEmpty());
        mvc.perform(put("/api/v1/admin/resources/{id}", resourceId).session(token.session())
                        .header("X-CSRF-TOKEN", token.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Poradnik opublikowany\",\"url\":\"https://example.org/two\","
                                + "\"kind\":\"PUBLIKACJE\",\"published\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));
        mvc.perform(get("/api/v1/resources"))
                .andExpect(jsonPath("$[?(@.id == " + resourceId + ")].name")
                        .value("Poradnik opublikowany"));

        MvcResult innovationResult = mvc.perform(post("/api/v1/admin/innovations")
                        .session(token.session()).header("X-CSRF-TOKEN", token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Testowa innowacja\",\"status\":\"ROZWOJ\",\"areaId\":"
                                + areaId + "}"))
                .andExpect(status().isCreated()).andReturn();
        long innovationId = ((Number) JsonPath.read(innovationResult.getResponse().getContentAsString(), "$.id"))
                .longValue();
        mvc.perform(get("/api/v1/innovations/{id}", innovationId)).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/admin/innovations/{id}", innovationId).session(token.session())
                        .header("X-CSRF-TOKEN", token.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Innowacja robocza\",\"status\":\"TESTOWANA\","
                                + "\"published\":false,\"areaId\":" + areaId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(false));
        mvc.perform(get("/api/v1/innovations/{id}", innovationId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/admin/innovations").session(admin))
                .andExpect(jsonPath("$.content[?(@.id == " + innovationId + ")].published").value(false));
        mvc.perform(put("/api/v1/admin/innovations/{id}", innovationId).session(token.session())
                        .header("X-CSRF-TOKEN", token.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Innowacja gotowa\",\"status\":\"WDROZONA\","
                                + "\"published\":true,\"areaId\":" + areaId + "}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/innovations/{id}", innovationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Innowacja gotowa"));
    }

    @Test
    void staffReceivesNewIdeaAndAuthorReceivesReply() throws Exception {
        MockHttpSession staff = login("staff-idea-flow@example.org", AppUserRole.STAFF);
        MockHttpSession author = login("author-idea-flow@example.org", AppUserRole.MEMBER);
        MockHttpSession stranger = login("stranger-idea-flow@example.org", AppUserRole.MEMBER);
        SessionCsrf authorToken = csrf(author);
        MvcResult created = mvc.perform(post("/api/v1/ideas").session(authorToken.session())
                        .header("X-CSRF-TOKEN", authorToken.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Ogród sąsiedzki\",\"stage\":\"MYSL\"}"))
                .andExpect(status().isCreated()).andReturn();
        long ideaId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(get("/api/v1/notifications").session(staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ideaId == " + ideaId + ")].kind").value("NEW_IDEA"));
        mvc.perform(get("/api/v1/staff/ideas").session(staff))
                .andExpect(jsonPath("$[?(@.id == " + ideaId + ")].title").value("Ogród sąsiedzki"));

        SessionCsrf staffToken = csrf(staff);
        mvc.perform(post("/api/v1/staff/ideas/{id}/replies", ideaId).session(staffToken.session())
                        .header("X-CSRF-TOKEN", staffToken.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Zapraszamy na konsultację.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Zapraszamy na konsultację."));
        MvcResult authorNotifications = mvc.perform(get("/api/v1/notifications").session(author))
                .andExpect(jsonPath("$[?(@.ideaId == " + ideaId + ")].kind").value("IDEA_REPLY"))
                .andReturn();
        long notificationId = ((Number) JsonPath.read(
                authorNotifications.getResponse().getContentAsString(), "$[0].id")).longValue();
        mvc.perform(patch("/api/v1/notifications/{id}/read", notificationId)
                        .session(authorToken.session()).header("X-CSRF-TOKEN", authorToken.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.read").value(true));
        SessionCsrf strangerToken = csrf(stranger);
        mvc.perform(patch("/api/v1/notifications/{id}/read", notificationId)
                        .session(strangerToken.session()).header("X-CSRF-TOKEN", strangerToken.token()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/ideas/{id}/replies", ideaId).session(author))
                .andExpect(jsonPath("$[0].body").value("Zapraszamy na konsultację."));
        mvc.perform(get("/api/v1/ideas/{id}/replies", ideaId).session(stranger))
                .andExpect(status().isNotFound());
    }

    private MockHttpSession login(String email, AppUserRole role) throws Exception {
        users.save(new AppUser(email, passwordEncoder.encode(PASSWORD), role.name(), role));
        return loginExisting(email, PASSWORD);
    }

    private MockHttpSession loginExisting(String email, String password) throws Exception {
        SessionCsrf guest = csrf();
        MvcResult result = mvc.perform(post("/login").session(guest.session())
                        .header("X-CSRF-TOKEN", guest.token())
                        .param("username", email).param("password", password))
                .andExpect(status().is3xxRedirection()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/v1/me").session(session))
                .andExpect(status().isOk());
        return session;
    }

    private SessionCsrf csrf() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/csrf")).andExpect(status().isOk()).andReturn();
        return new SessionCsrf((MockHttpSession) result.getRequest().getSession(false),
                JsonPath.read(result.getResponse().getContentAsString(), "$.token"));
    }

    private SessionCsrf csrf(MockHttpSession session) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        return new SessionCsrf(session, JsonPath.read(result.getResponse().getContentAsString(), "$.token"));
    }

    private record SessionCsrf(MockHttpSession session, String token) {}
}
