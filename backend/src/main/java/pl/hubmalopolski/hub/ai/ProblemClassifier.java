package pl.hubmalopolski.hub.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import pl.hubmalopolski.hub.domain.ChallengeArea;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Przypisuje zgloszenie do obszaru wyzwan (Mapa Wyzwan Spolecznych).
 * Bez structured-output: model zwraca samo id (1 token dekodowania zamiast
 * JSON-u + schematu w prompcie — CPU llama.cpp liczy kazdy token ~80 ms).
 */
@Service
public class ProblemClassifier {

    private static final Pattern ID = Pattern.compile("\\d+");
    private final ChatClient chatClient;

    public ProblemClassifier(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChallengeArea classify(String problemText, List<ChallengeArea> areas) {
        try {
            StringBuilder sb = new StringBuilder("Obszary wyzwan (id: nazwa):\n");
            areas.forEach(a -> sb.append(a.getId()).append(": ").append(a.getName()).append("\n"));
            String answer = chatClient.prompt()
                    .system("Wybierz JEDEN najbardziej pasujacy obszar. Odpowiedz WYLACZNIE "
                            + "liczba (id obszaru), bez zadnych innych slow.")
                    .user(sb + "\nZgloszony problem: " + problemText)
                    .options(OpenAiChatOptions.builder().maxTokens(8).temperature(0.0))
                    .call().content();
            if (answer != null) {
                Matcher m = ID.matcher(answer);
                if (m.find()) {
                    Long id = Long.valueOf(m.group());
                    return areas.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
                }
            }
        } catch (Exception ignored) {
            // bez LLM zostawiamy raport bez obszaru — admin przypisze recznie
        }
        return null;
    }
}
