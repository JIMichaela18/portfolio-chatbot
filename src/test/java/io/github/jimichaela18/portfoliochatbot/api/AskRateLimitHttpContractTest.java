package io.github.jimichaela18.portfoliochatbot.api;

import io.github.jimichaela18.portfoliochatbot.chat.PortfolioChatService;
import io.github.jimichaela18.portfoliochatbot.error.GlobalApiExceptionHandler;
import io.github.jimichaela18.portfoliochatbot.security.AskRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortfolioChatController.class)
@Import({AskRateLimitFilter.class, GlobalApiExceptionHandler.class})
class AskRateLimitHttpContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioChatService chatService;

    @Test
    void returnsTooManyRequestsContractAfterTenRequests() throws Exception {
        when(chatService.answerQuestion(anyString())).thenReturn("Answer");

        for (int request = 0; request < 10; request++) {
            mockMvc.perform(post("/ask")
                            .with(remoteAddress("192.0.2.10"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"question\":\"A valid question\"}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/ask")
                        .with(remoteAddress("192.0.2.10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"A valid question\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", matchesPattern("[1-9][0-9]*")))
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(jsonPath("$.error").value("Too many requests"))
                .andExpect(content().string("{\"error\":\"Too many requests\"}"));
    }

    private RequestPostProcessor remoteAddress(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}
