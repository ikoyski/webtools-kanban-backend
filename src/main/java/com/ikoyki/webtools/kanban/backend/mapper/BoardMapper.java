package com.ikoyki.webtools.kanban.backend.mapper;

import com.ikoyki.webtools.kanban.backend.entity.BoardEntity;
import com.ikoyki.webtools.kanban.backend.entity.ColumnEntity;
import com.ikoyki.webtools.kanban.backend.entity.CardEntity;
import com.ikoyki.webtools.kanban.backend.dto.response.BoardResponse;
import com.ikoyki.webtools.kanban.backend.dto.response.ColumnResponse;
import com.ikoyki.webtools.kanban.backend.dto.response.CardResponse;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class BoardMapper {

    public BoardResponse toResponse(BoardEntity board, com.ikoyki.webtools.kanban.backend.entity.BoardRole userRole) {
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

                // 1. Extract all Card IDs to satisfy the column's cardIds layout
                List<UUID> cardIds = Optional.ofNullable(column.getCards())
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(Objects::nonNull)
                        .map(CardEntity::getId)
                        .collect(Collectors.toList());

                // 2. Build the flat Column Response
                ColumnResponse columnResponse = ColumnResponse.builder()
                        .id(column.getId())
                        .title(column.getTitle())
                        .position(column.getPosition())
                        .cardIds(cardIds)
                        .build();

                columnMap.put(column.getId(), columnResponse);

                // 3. Map nested cards into the single root-level cards dictionary
                if (column.getCards() != null) {
                    for (CardEntity card : column.getCards()) {
                        if (card == null)
                            continue;

                        CardResponse cardResponse = toCardResponse(card);
                        cardMap.put(card.getId(), cardResponse);
                    }
                }
            }
        }

        // 4. Construct and return final BoardResponse matching frontend state
        // properties
        return BoardResponse.builder()
                .id(board.getId())
                .name(board.getName())
                .columns(columnMap)
                .cards(cardMap)
                .settings(Map.of("theme", "light"))
                .role(userRole)
                .build();
    }


    public ColumnResponse toColumnResponse(ColumnEntity column) {
        if (column == null) {
            return null;
        }

        // Pulling all card IDs to satisfy the updated frontend state normalization
        List<UUID> cardIds = Optional.ofNullable(column.getCards())
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
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
                .build();
    }
}
