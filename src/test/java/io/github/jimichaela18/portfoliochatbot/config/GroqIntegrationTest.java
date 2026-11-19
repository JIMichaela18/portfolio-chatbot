package io.github.jimichaela18.portfoliochatbot.config;

import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GroqIntegrationTest {

    @Test
    void shouldGenerateResponseFromGroq() {

        String apiKey = System.getenv("GQ_LLM_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), "GQ_LLM_API_KEY is missing");

        ChatModel model = new GroqChatModelConfiguration().groqChatModel(apiKey);

        String response = model.chat(
                "Explain what Spring Boot is in two sentences."
        );

        assertNotNull(response);
        assertFalse(response.isBlank());

        System.out.println("LLM Response: " + response);
    }
}