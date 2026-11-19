package io.github.jimichaela18.portfoliochatbot.api;

import io.github.jimichaela18.portfoliochatbot.chat.ChatUnavailableException;
import io.github.jimichaela18.portfoliochatbot.chat.PortfolioChatService;
import io.github.jimichaela18.portfoliochatbot.error.GlobalApiExceptionHandler;
import io.github.jimichaela18.portfoliochatbot.security.AskRateLimitFilter;
import io.github.jimichaela18.portfoliochatbot.security.AskRequestBodySizeFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortfolioChatController.class)
@Import({AskRequestBodySizeFilter.class, AskRateLimitFilter.class, GlobalApiExceptionHandler.class})
class PortfolioChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioChatService chatService;

    private RequestPostProcessor remoteAddr(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    @Test
    void answersValidQuestion() throws Exception {
        when(chatService.answerQuestion("What are her technical skills?")).thenReturn("Java and Spring Boot.");

        mockMvc.perform(post("/ask")
                        .with(remoteAddr("192.0.2.1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"  What are her technical skills?  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Java and Spring Boot."));

        verify(chatService).answerQuestion("What are her technical skills?");
    }

    @Test
    void rejectsBlankQuestion() throws Exception {
        mockMvc.perform(post("/ask")
                        .with(remoteAddr("192.0.2.2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Question must not be blank"));
    }

    @Test
    void hidesChatProviderFailureDetails() throws Exception {
        RuntimeException providerFailure = new RuntimeException("secret provider failure");
        when(chatService.answerQuestion("question")).thenThrow(
                new ChatUnavailableException("The chat provider is unavailable", providerFailure)
        );

        mockMvc.perform(post("/ask")
                        .with(remoteAddr("192.0.2.4"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"question\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value("The portfolio assistant is temporarily unavailable"));
    }

    @Test
    void unrelatedRuntimeFailureReturnsInternalServerError() throws Exception {
        when(chatService.answerQuestion("question")).thenThrow(new RuntimeException("unexpected failure"));

        mockMvc.perform(post("/ask")
                        .with(remoteAddr("192.0.2.5"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"question\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/ask")
                        .with(remoteAddr("192.0.2.6"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request body is invalid or missing"));
    }

    @Test
    void rejectsQuestionsWithOnlyWhitespace() throws Exception {
        mockMvc.perform(post("/ask")
                        .with(remoteAddr("192.0.2.8"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"      \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Question must not be blank"));
    }

    @Test
    void acceptsQuestionsWithUnicodeCharacters() throws Exception {
        String question = "¿Cuál es tu nombre?";
        when(chatService.answerQuestion(question)).thenReturn("Mi nombre es TOB.");

        mockMvc.perform(post("/ask")
                        .with(remoteAddr("192.0.2.9"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + question + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Mi nombre es TOB."));

        verify(chatService).answerQuestion(question);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NUL", "TAB", "LF", "CR", "DEL"})
    void rejectsControlCharacters(String controlCharacterName) throws Exception {
        String jsonControlCharacter = switch (controlCharacterName) {
            case "NUL" -> "\\u0000";
            case "TAB" -> "\\t";
            case "LF" -> "\\n";
            case "CR" -> "\\r";
            case "DEL" -> "\\u007f";
            default -> throw new IllegalArgumentException("Unknown control character: " + controlCharacterName);
        };

        mockMvc.perform(post("/ask")
                        .with(remoteAddr("192.0.2.20"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Invalid" + jsonControlCharacter + "question\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Question must not contain control characters"));
    }

    @Test
    void rejectsUnknownJsonProperties() throws Exception {
        String invalidJson = "{\"question\":\"What is Java?\", \"unknownProperty\":\"value\"}";

        mockMvc.perform(post("/ask")
                        .with(remoteAddr("192.0.2.11"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request body is invalid or missing"));

        verify(chatService, never()).answerQuestion(anyString());
    }

    @ParameterizedTest
    @CsvSource({"300, 200", "301, 400"})
    void validatesQuestionLengthAfterTrimming(int length, int expectedStatus) throws Exception {
        String question = "a".repeat(length);

        if (expectedStatus == 200) {
            when(chatService.answerQuestion(question)).thenReturn("Answer");
        }

        var result = mockMvc.perform(post("/ask")
                        .with(remoteAddr("192.0.2.3"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"   " + question + "   \"}"))
                .andExpect(status().is(expectedStatus));

        if (expectedStatus == 200) {
            result.andExpect(jsonPath("$.answer").value("Answer"));
            verify(chatService).answerQuestion(question);
        } else {
            result.andExpect(jsonPath("$.message").value("Question must not exceed 300 characters"));
        }
    }
}
