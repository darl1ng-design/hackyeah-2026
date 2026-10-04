package pl.hubmalopolski.hub.transcription;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.hubmalopolski.hub.config.SecurityConfig;
import pl.hubmalopolski.hub.repo.AppUserRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TranscriptionController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "hub.transcription.per-minute=3")
class TranscriptionApiTests {
    @Autowired MockMvc mvc;
    @MockitoBean TranscriptionService transcription;
    @MockitoBean AppUserRepository users;

    private static MockMultipartFile webm(int size) {
        return new MockMultipartFile("file", "blob", "audio/webm;codecs=opus", new byte[size]);
    }

    @Test
    void guestTranscribesWebmWithoutLogin() throws Exception {
        when(transcription.available()).thenReturn(true);
        when(transcription.transcribe(any(), eq("audio.webm"), eq("pl"))).thenReturn("Dzień dobry");

        mvc.perform(multipart("/api/v1/transcribe").file(webm(100)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Dzień dobry"));
        verify(transcription).transcribe(any(), eq("audio.webm"), eq("pl"));
    }

    @Test
    void rejectsMissingCsrfEmptyUnsupportedAndBadLanguage() throws Exception {
        when(transcription.available()).thenReturn(true);
        mvc.perform(multipart("/api/v1/transcribe").file(webm(100))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/v1/transcribe").file(webm(0)).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/transcribe").with(csrf())
                        .file(new MockMultipartFile("file", "x.txt", "text/plain", new byte[10])))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(multipart("/api/v1/transcribe").file(webm(100)).param("language", "xx").with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportsUnavailableBackendAs503() throws Exception {
        when(transcription.available()).thenReturn(true);
        when(transcription.transcribe(any(), any(), any())).thenThrow(new RuntimeException("connect timeout"));
        mvc.perform(multipart("/api/v1/transcribe").file(webm(100)).with(csrf()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Nie udało się rozpoznać nagrania. Spróbuj ponownie za chwilę."));
    }

    @Test
    void healthIsPublic() throws Exception {
        when(transcription.available()).thenReturn(true);
        mvc.perform(get("/api/v1/transcribe/health")).andExpect(jsonPath("$.available").value(true));
    }
}
