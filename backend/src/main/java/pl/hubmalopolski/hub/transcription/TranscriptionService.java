package pl.hubmalopolski.hub.transcription;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

/**
 * Speech-to-text through an OpenAI-compatible endpoint (LiteLLM at api.skalujto.ai →
 * faster-whisper on DGX-A). Configured via spring.ai.openai.audio.transcription.*.
 */
@Service
public class TranscriptionService {
    /** Biases Whisper towards the domain vocabulary users dictate in forms. */
    static final String PROMPT = "Zgłoszenie mieszkańca Małopolski o problemie społecznym lub pomyśle "
            + "na innowację społeczną: seniorzy, gmina, sołectwo, ośrodek pomocy społecznej, ROPS.";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ObjectProvider<TranscriptionModel> model;
    private final String modelName;

    // Model name set per request: Spring AI 2.0.1 otherwise sends its default "whisper-1".
    public TranscriptionService(ObjectProvider<TranscriptionModel> model,
                                @Value("${hub.transcription.model:whisper-large-v3-turbo}") String modelName) {
        this.model = model;
        this.modelName = modelName;
    }

    public boolean available() {
        return model.getIfAvailable() != null;
    }

    public String transcribe(byte[] audio, String filename, String language) {
        TranscriptionModel m = model.getIfAvailable();
        if (m == null) throw new IllegalStateException("transcription model not configured");
        var resource = new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return filename; // the extension tells the server how to decode it
            }
        };
        var options = OpenAiAudioTranscriptionOptions.builder()
                .model(modelName)
                .language(language)
                .prompt(PROMPT)
                .build();
        return textOf(m.call(new AudioTranscriptionPrompt(resource, options)).getResult().getOutput());
    }

    /** LiteLLM returns the JSON body verbatim as the output (whatever response_format says); unwrap it. */
    static String textOf(String output) {
        String s = output.trim();
        if (!s.startsWith("{")) return s;
        try {
            return JSON.readTree(s).path("text").asText("").trim();
        } catch (Exception e) {
            return s;
        }
    }
}
