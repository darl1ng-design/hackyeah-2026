package pl.hubmalopolski.hub.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.hubmalopolski.hub.account.AccountService;
import pl.hubmalopolski.hub.config.SecurityConfig;
import pl.hubmalopolski.hub.domain.AppUser;
import pl.hubmalopolski.hub.domain.AppUserRole;
import pl.hubmalopolski.hub.repo.AppUserRepository;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountApiTests {
    @Autowired MockMvc mvc;
    @MockitoBean AccountService accounts;
    @MockitoBean AppUserRepository users;

    @Test
    void guestCanRegisterWithoutExposingPassword() throws Exception {
        when(accounts.register(anyString(), anyString(), anyString())).thenReturn(
                new AppUser("anna@example.org", "secret-hash", "Anna", AppUserRole.MEMBER));

        mvc.perform(post("/api/v1/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"anna@example.org\",\"password\":\"very-long-password\",\"displayName\":\"Anna\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("anna@example.org"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registrationRequiresCsrfToken() throws Exception {
        mvc.perform(post("/api/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"anna@example.org\",\"password\":\"very-long-password\",\"displayName\":\"Anna\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void weakPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/v1/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"anna@example.org\",\"password\":\"123\",\"displayName\":\"Anna\"}"))
                .andExpect(status().isBadRequest());
    }
}
