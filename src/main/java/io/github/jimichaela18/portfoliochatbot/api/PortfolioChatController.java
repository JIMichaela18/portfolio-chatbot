package io.github.jimichaela18.portfoliochatbot.api;

import io.github.jimichaela18.portfoliochatbot.chat.PortfolioChatService;
import io.github.jimichaela18.portfoliochatbot.dto.AskRequest;
import io.github.jimichaela18.portfoliochatbot.dto.AskResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortfolioChatController {

    private final PortfolioChatService chatService;

    public PortfolioChatController(PortfolioChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return new AskResponse(chatService.answerQuestion(request.question()));
    }
}