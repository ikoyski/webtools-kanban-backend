package com.ikoyki.webtools.kanban.backend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void preservesResponseStatusExceptionStatus() {
        var response = handler.handleRuntime(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid central identity claims"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("UNAUTHORIZED", response.getBody().getError());
        assertEquals("Invalid central identity claims", response.getBody().getMessage());
    }

    @Test
    void preservesStatusFromAnnotatedDomainExceptions() {
        var response = handler.handleRuntime(new ForbiddenBoardAccessException("Not a board member"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("FORBIDDEN", response.getBody().getError());
    }

    @Test
    void unexpectedRuntimeExceptionsRemainInternalServerErrors() {
        var response = handler.handleRuntime(new IllegalStateException("Unexpected failure"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getError());
    }
}
