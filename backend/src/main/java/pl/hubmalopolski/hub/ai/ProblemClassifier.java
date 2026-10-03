package pl.hubmalopolski.hub.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import pl.hubmalopolski.hub.domain.ChallengeArea;

import java.util.List;

/**
 * Przypisuje zgloszenie do obszaru wyzwan (Mapa Wyzwan Spolecznych) — structured output.
 */
@Service
public class ProblemClassifier {

    private final ChatClient chatClient;

    public ProblemClassifier(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChallengeArea classify(String problemText, List<ChallengeArea> areas) {
        try {
            StringBuilder sb = new StringBuilder("Obszary wyzwan (id: nazwa):\n");
            areas.forEach(a -> sb.append(a.getId()).append(": ").append(a.getName()).append("\n"));
            AreaChoice choice = chatClient.prompt()
                    .system("Wybierz JEDEN najbardziej pasujacy obszar. Zwracaj wylacznie id.")
                    .user(sb + "\nZgloszony problem: " + problemText)
                    .call()
                    .entity(AreaChoice.class);
            if (choice != null && choice.areaId() != null) {
                return areas.stream().filter(a -> a.getId().equals(choice.areaId())).findFirst().orElse(null);
            }
        } catch (Exception ignored) {
            // bez LLM zostawiamy raport bez obszaru — admin przypisze recznie
        }
        return null;
    }

    public record AreaChoice(Long areaId) {
    }
}
