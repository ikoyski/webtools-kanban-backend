package com.ikoyki.webtools.kanban.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private String error;
        private String message;
        private Integer cardCount;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .error("NOT_FOUND")
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(ColumnNotEmptyException.class)
    public ResponseEntity<ErrorResponse> handleColumnNotEmpty(ColumnNotEmptyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.builder()
                        .error("COLUMN_NOT_EMPTY")
                        .message(ex.getMessage())
                        .cardCount(ex.getCardCount())
                        .build());
    }

    @ExceptionHandler(InvalidImportException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImport(InvalidImportException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .error("INVALID_IMPORT")
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        if (ex instanceof ResponseStatusException responseStatusException) {
            HttpStatusCode status = responseStatusException.getStatusCode();
            HttpStatus resolved = HttpStatus.resolve(status.value());
            return ResponseEntity.status(status)
                    .body(ErrorResponse.builder()
                            .error(resolved == null ? "HTTP_" + status.value() : resolved.name())
                            .message(responseStatusException.getReason() == null
                                    ? status.toString() : responseStatusException.getReason())
                            .build());
        }

        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(
                ex.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            HttpStatus status = responseStatus.code();
            return ResponseEntity.status(status)
                    .body(ErrorResponse.builder()
                            .error(status.name())
                            .message(responseStatus.reason().isBlank()
                                    ? ex.getMessage() : responseStatus.reason())
                            .build());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .error("INTERNAL_SERVER_ERROR")
                        .message(ex.getMessage())
                        .build());
    }
}
