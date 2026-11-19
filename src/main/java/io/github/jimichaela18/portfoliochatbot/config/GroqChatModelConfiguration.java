package io.github.jimichaela18.portfoliochatbot.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GroqChatModelConfiguration {

    @Bean
    public ChatModel groqChatModel(@Value("${GQ_LLM_API_KEY}") String apiKey) {
        return OpenAiChatModel.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .apiKey(apiKey)
                .modelName("openai/gpt-oss-20b")
                .temperature(0.2)
                .build();
    }
}