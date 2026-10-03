package pl.hubmalopolski.hub.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** Asystent kreatora innowacji — prowadzi uzytkownika przez Canwe Innowacji Spolecznych. */
@Service
public class IdeaAssistant {

    private static final String SYSTEM = String.join("\n",
        "Jestes asystentem kreatorem innowacji spolecznych w Malopolskim Hubie Innowacji Spolecznych.",
        "Pomozesz mieszkankom i organizacjom rozwinac pomysl wedlug Canwy Innowacji Spolecznych:",
        "problem i jego odbiorcy, wartosc, zasoby, dzialania, partnerzy, skutki i wskazniki.",
        "Zadawaj maksymalnie 2 konkretne pytania naraz, podawaj przyklady z Malopolski,",
        "proponuj nietuzinkowe rozwiazania. Odpowiadaj po polsku, zyciowo i bez zargonu urzedniczego.");

    private final ChatClient chatClient;

    public IdeaAssistant(ChatClient chatClient) { this.chatClient = chatClient; }

    public String coach(String userMessage) {
        return chatClient.prompt()
                .system(SYSTEM)
                .user(userMessage)
                .call()
                .content();
    }
}
