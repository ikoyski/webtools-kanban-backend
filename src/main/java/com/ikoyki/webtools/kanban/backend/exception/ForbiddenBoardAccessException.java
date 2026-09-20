package com.ikoyki.webtools.kanban.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenBoardAccessException extends RuntimeException {
    public ForbiddenBoardAccessException(String message) {
        super(message);
    }
}
