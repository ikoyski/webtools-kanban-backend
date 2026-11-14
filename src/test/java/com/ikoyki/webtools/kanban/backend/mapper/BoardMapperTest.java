package com.ikoyki.webtools.kanban.backend.mapper;

import com.ikoyki.webtools.kanban.backend.dto.response.BoardResponse;
import com.ikoyki.webtools.kanban.backend.dto.response.ColumnResponse;
import com.ikoyki.webtools.kanban.backend.entity.BoardEntity;
import com.ikoyki.webtools.kanban.backend.entity.BoardRole;
import com.ikoyki.webtools.kanban.backend.entity.CardEntity;
import com.ikoyki.webtools.kanban.backend.entity.ColumnEntity;
import com.ikoyki.webtools.kanban.backend.entity.Priority;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BoardMapperTest {

    private final BoardMapper boardMapper = new BoardMapper();

    @Test
    void toResponse_excludesArchivedCardsFromColumnAndBoardCards() {
        UUID boardId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();
        UUID activeCardId = UUID.randomUUID();
        UUID archivedCardId = UUID.randomUUID();

        CardEntity activeCard = createCard(activeCardId, false, 0);
        CardEntity archivedCard = createCard(archivedCardId, true, 1);

        ColumnEntity column = ColumnEntity.builder()
                .id(columnId)
                .title("To Do")
                .position(0)
                .cards(List.of(activeCard, archivedCard))
                .build();

        BoardEntity board = BoardEntity.builder()
                .id(boardId)
                .name("Test Board")
                .columns(List.of(column))
                .build();

        BoardResponse response = boardMapper.toResponse(board, BoardRole.OWNER);

        assertNotNull(response);
        assertEquals(List.of(activeCardId), response.getColumns().get(columnId).getCardIds());
        assertEquals(Set.of(activeCardId), response.getCards().keySet());
        assertNotNull(response.getCards().get(activeCardId));
    }

    @Test
    void toColumnResponse_excludesArchivedCards() {
        UUID columnId = UUID.randomUUID();
        UUID activeCardId = UUID.randomUUID();
        UUID archivedCardId = UUID.randomUUID();

        ColumnEntity column = ColumnEntity.builder()
                .id(columnId)
                .title("To Do")
                .position(0)
                .cards(List.of(
                        createCard(activeCardId, false, 0),
                        createCard(archivedCardId, true, 1)))
                .build();

        ColumnResponse response = boardMapper.toColumnResponse(column);

        assertNotNull(response);
        assertEquals(List.of(activeCardId), response.getCardIds());
    }

    private CardEntity createCard(UUID id, boolean archived, int position) {
        return CardEntity.builder()
                .id(id)
                .title(archived ? "Archived card" : "Active card")
                .description("")
                .priority(Priority.MEDIUM)
                .labels(List.of())
                .position(position)
                .archived(archived)
                .build();
    }
}
