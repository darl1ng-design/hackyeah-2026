package pl.hubmalopolski.hub.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class ChatConfig {
    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        // enable_thinking=false: backend LLM (tabbyAPI/Qwen3) to bez trybu
        // rozumowania — inaczej reasoning_content zjada caly max_tokens i
        // 'content' przychodzi pusty. llama.cpp ignoruje te extra pole.
        return ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .extraBody(Map.of("enable_thinking", false)))
                .build();
    }
}
