package io.github.jimichaela18.portfoliochatbot.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class GroqChatModelConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GroqChatModelConfiguration.class);

    @Bean
    public ChatModel groqChatModel(@Value("${GQ_LLM_API_KEY}") String apiKey) {
        try {
            ChatModel model = OpenAiChatModel.builder()
                    .baseUrl("https://api.groq.com/openai/v1")
                    .apiKey(apiKey)
                    .modelName("openai/gpt-oss-20b")
                    .temperature(0.2)
                    .build();
            log.info("Groq chat model initialized: model={}", "openai/gpt-oss-20b");
            return model;
        } catch (RuntimeException exception) {
            log.error("Groq chat model initialization failed", exception);
            throw exception;
        }
    }
}