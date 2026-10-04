package pl.hubmalopolski.hub.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import pl.hubmalopolski.hub.domain.IdeaStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdeaStoryParserTests {
    @Test
    void splitsIdeaStoryWithProviderStructuredOutputAndSchemaValidation() {
        ChatClient chat = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
        ChatClient.EntityParamSpec options = mock(ChatClient.EntityParamSpec.class);
        var parsed = new IdeaStoryParser.ParsedIdea(" Bus na telefon ", "Istota.", "seniorzy", null, " ");
        when(chat.prompt()).thenReturn(request);
        when(request.system(anyString())).thenReturn(request);
        when(request.user(anyString())).thenReturn(request);
        when(request.call()).thenReturn(response);
        when(options.useProviderStructuredOutput()).thenReturn(options);
        when(options.validateSchema()).thenReturn(options);
        when(response.entity(eq(IdeaStoryParser.ParsedIdea.class), any())).thenAnswer(invocation -> {
            invocation.<java.util.function.Consumer<ChatClient.EntityParamSpec>>getArgument(1).accept(options);
            return parsed;
        });

        var p = new IdeaStoryParser(chat).parse("Mam pomysł na bus dla seniorów.");

        verify(response).entity(eq(IdeaStoryParser.ParsedIdea.class), any());
        verify(options).useProviderStructuredOutput();
        verify(options).validateSchema();
        assertThat(p.title()).isEqualTo("Bus na telefon");
        assertThat(p.essence()).isEqualTo("Istota.");
        assertThat(p.targetGroup()).isEqualTo("seniorzy");
        assertThat(p.stage()).isEqualTo(IdeaStage.MYSL);
        assertThat(p.description()).isEqualTo("Mam pomysł na bus dla seniorów.");
    }

    @Test
    void adaptationFallsBackToStoryForNeedAndTrimsFields() {
        ChatClient chat = mock(ChatClient.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(chat.prompt().system(anyString()).user(anyString()).call()
                .entity(eq(IdeaStoryParser.ParsedAdaptation.class), any()))
                .thenReturn(new IdeaStoryParser.ParsedAdaptation(" Klub ", " GOPS ", null, " ", null));

        var p = new IdeaStoryParser(chat).parseAdaptation("Chcemy klub w GOPS.");

        assertThat(p.innovation()).isEqualTo("Klub");
        assertThat(p.institution()).isEqualTo("GOPS");
        assertThat(p.targetGroup()).isEmpty();
        assertThat(p.need()).isEqualTo("Chcemy klub w GOPS.");
        assertThat(p.constraints()).isEmpty();
    }
}
