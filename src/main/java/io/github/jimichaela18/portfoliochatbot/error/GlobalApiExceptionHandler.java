package io.github.jimichaela18.portfoliochatbot.error;

import io.github.jimichaela18.portfoliochatbot.chat.ChatUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        log.debug("Request validation failed: fieldErrors={}",
                exception.getBindingResult().getFieldErrors().size());
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");
        return error(HttpStatus.BAD_REQUEST, "Bad Request", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        log.debug("Malformed or missing JSON request body");
        return error(HttpStatus.BAD_REQUEST, "Bad Request", "Request body is invalid or missing");
    }

    @ExceptionHandler(ChatUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleChatFailure(ChatUnavailableException exception) {
        log.warn("TOB chat provider unavailable");
        return error(HttpStatus.BAD_GATEWAY, "Bad Gateway", "The portfolio assistant is temporarily unavailable");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedFailure(Exception exception) {
        log.error("Unexpected API failure", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred");
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(status.value(), error, message));
    }
}