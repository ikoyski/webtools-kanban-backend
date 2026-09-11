package com.ikoyki.webtools.kanban.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
@Getter
public class ColumnNotEmptyException extends RuntimeException {
    private final int cardCount;

    public ColumnNotEmptyException(int cardCount) {
        super("Column has " + cardCount + " card(s). Provide transferTo to move them first.");
        this.cardCount = cardCount;
    }
}
