package pl.hubmalopolski.hub.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import pl.hubmalopolski.hub.domain.IdeaStage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdeaAssistantTests {
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void promptIncludesPreviousExchangeAndIdeaContextInOrder() {
        ChatClient client = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
        when(client.prompt()).thenReturn(request);
        when(request.messages(anyList())).thenReturn(request);
        when(request.call()).thenReturn(response);
        when(response.content()).thenReturn("Dalsza porada");
        IdeaAssistant assistant = new IdeaAssistant(client);

        String reply = assistant.coach("Co dalej?", List.of(
                new IdeaAssistant.Turn(IdeaAssistant.Role.USER, "Mam pomysł"),
                new IdeaAssistant.Turn(IdeaAssistant.Role.ASSISTANT, "Kogo dotyczy?")),
                new IdeaAssistant.IdeaContext("Mobilna pomoc", null, "Seniorzy", IdeaStage.PROTOTYP, null));

        org.mockito.ArgumentCaptor<List<Message>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(request).messages(captor.capture());
        List<Message> messages = captor.getValue();
        assertEquals("Dalsza porada", reply);
        assertEquals(5, messages.size());
        assertInstanceOf(SystemMessage.class, messages.get(0));
        assertTrue(((AbstractMessage) messages.get(1)).getText().contains("Seniorzy"));
        assertInstanceOf(UserMessage.class, messages.get(2));
        assertEquals("Mam pomysł", ((AbstractMessage) messages.get(2)).getText());
        assertInstanceOf(AssistantMessage.class, messages.get(3));
        assertEquals("Kogo dotyczy?", ((AbstractMessage) messages.get(3)).getText());
        assertEquals("Co dalej?", ((AbstractMessage) messages.get(4)).getText());
    }
}
