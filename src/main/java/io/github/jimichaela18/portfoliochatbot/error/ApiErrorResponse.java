package io.github.jimichaela18.portfoliochatbot.error;

public record ApiErrorResponse(int status, String error, String message) {
}