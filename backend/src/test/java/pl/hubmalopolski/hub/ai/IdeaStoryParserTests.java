package pl.hubmalopolski.hub.ai;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import pl.hubmalopolski.hub.domain.IdeaStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdeaStoryParserTests {
    @Test
    void fallsBackToStoryWhenModelDropsDescriptionAndDefaultsStage() {
        ChatClient chat = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        when(chat.prompt().system(anyString()).user(anyString()).call().entity(any(Class.class)))
                .thenReturn(new IdeaStoryParser.ParsedIdea(" Bus na telefon ", "Istota.", null, null, " "));
        var p = new IdeaStoryParser(chat).parse("Mam pomysł na bus.");
        assertThat(p.title()).isEqualTo("Bus na telefon");
        assertThat(p.targetGroup()).isEmpty();
        assertThat(p.stage()).isEqualTo(IdeaStage.MYSL);
        assertThat(p.description()).isEqualTo("Mam pomysł na bus.");
    }
}
