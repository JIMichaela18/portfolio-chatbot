package io.github.jimichaela18.portfoliochatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AskRequest(
        @NotBlank(message = "Question must not be blank")
        @Size(max = 300, message = "Question must not exceed 300 characters")
        @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "Question must not contain control characters")
        String question
) {
    public AskRequest {
        question = question == null ? null : question.strip();
    }
}