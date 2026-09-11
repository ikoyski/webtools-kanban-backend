package com.ikoyki.webtools.kanban.backend.mapper;

import com.ikoyki.webtools.kanban.backend.dto.response.*;
import com.ikoyki.webtools.kanban.backend.entity.*;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BoardMapper {
    public BoardResponse toResponse(Board board) {
        if (board == null) return null;
        return BoardResponse.builder()
                .id(board.getId())
                .name(board.getName())
                .columns(mapColumns(board.getColumns()))
                .build();
    }

    private List<ColumnResponse> mapColumns(List<ColumnEntity> columns) {
        if (columns == null) return null;
        return columns.stream()
                .map(this::toColumnResponse)
                .collect(Collectors.toList());
    }

    public ColumnResponse toColumnResponse(ColumnEntity column) {
        if (column == null) return null;
        return ColumnResponse.builder()
                .id(column.getId())
                .title(column.getTitle())
                .position(column.getPosition())
                .cards(mapCards(column.getCards()))
                .build();
    }

    private List<CardResponse> mapCards(List<Card> cards) {
        if (cards == null) return null;
        return cards.stream()
                .map(this::toCardResponse)
                .collect(Collectors.toList());
    }

    public CardResponse toCardResponse(Card card) {
        if (card == null) return null;
        return CardResponse.builder()
                .id(card.getId())
                .title(card.getTitle())
                .description(card.getDescription())
                .priority(card.getPriority())
                .dueDate(card.getDueDate())
                .labels(card.getLabels())
                .position(card.getPosition())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }
}
