package com.ikoyki.webtools.kanban.backend.mapper;

import com.ikoyki.webtools.kanban.backend.entity.BoardEntity;
import com.ikoyki.webtools.kanban.backend.entity.ColumnEntity;
import com.ikoyki.webtools.kanban.backend.entity.CardEntity;
import com.ikoyki.webtools.kanban.backend.entity.BoardRole;
import com.ikoyki.webtools.kanban.backend.dto.response.BoardResponse;
import com.ikoyki.webtools.kanban.backend.dto.response.ColumnResponse;
import com.ikoyki.webtools.kanban.backend.dto.response.CardResponse;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class BoardMapper {

    public BoardResponse toResponse(BoardEntity board, BoardRole userRole) {
        if (board == null) {
            return null;
        }

        // Initialize our target top-level maps
        Map<UUID, ColumnResponse> columnMap = new LinkedHashMap<>();
        Map<UUID, CardResponse> cardMap = new LinkedHashMap<>();

        if (board.getColumns() != null) {
            for (ColumnEntity column : board.getColumns()) {
                if (column == null)
                    continue;

                // Include only active cards in the column's cardIds layout.
                List<UUID> cardIds = Optional.ofNullable(column.getCards())
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(card -> !card.isArchived())
                        .map(CardEntity::getId)
                        .collect(Collectors.toList());

                // Build the flat Column Response
                ColumnResponse columnResponse = ColumnResponse.builder()
                        .id(column.getId())
                        .title(column.getTitle())
                        .position(column.getPosition())
                        .cardIds(cardIds)
                        .build();

                columnMap.put(column.getId(), columnResponse);

                // Map active cards into the single root-level cards dictionary.
                if (column.getCards() != null) {
                    for (CardEntity card : column.getCards()) {
                        if (card == null || card.isArchived())
                            continue;

                        CardResponse cardResponse = toCardResponse(card);
                        cardMap.put(card.getId(), cardResponse);
                    }
                }
            }
        }

        // Construct and return final BoardResponse matching frontend state properties.
        return BoardResponse.builder()
                .id(board.getId())
                .name(board.getName())
                .columns(columnMap)
                .cards(cardMap)
                .role(userRole)
                .build();
    }

    public ColumnResponse toColumnResponse(ColumnEntity column) {
        if (column == null) {
            return null;
        }

        // Include only active cards in standalone column responses as well.
        List<UUID> cardIds = Optional.ofNullable(column.getCards())
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .filter(card -> !card.isArchived())
                .map(CardEntity::getId)
                .collect(Collectors.toList());

        return ColumnResponse.builder()
                .id(column.getId())
                .title(column.getTitle())
                .position(column.getPosition())
                .cardIds(cardIds)
                .build();
    }

    public CardResponse toCardResponse(CardEntity card) {
        if (card == null)
            return null;

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
                .archived(card.isArchived())
                .archivedAt(card.getArchivedAt())
                .build();
    }
}
