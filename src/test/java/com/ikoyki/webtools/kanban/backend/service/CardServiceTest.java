package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.entity.*;
import com.ikoyki.webtools.kanban.backend.exception.*;
import com.ikoyki.webtools.kanban.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private ColumnRepository columnRepository;
    @Mock private BoardAccessService boardAccessService;

    @InjectMocks
    private CardService cardService;

    private UUID columnId;
    private UUID cardId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        columnId = UUID.randomUUID();
        cardId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void createCard_Forbidden_ThrowsException() {
        ColumnEntity column = new ColumnEntity();
        BoardEntity board = new BoardEntity();
        board.setId(UUID.randomUUID());
        column.setBoard(board);

        when(columnRepository.findById(columnId)).thenReturn(Optional.of(column));
        doThrow(new ForbiddenBoardAccessException("Forbidden")).when(boardAccessService).requireAtLeast(board.getId(), userId, BoardRole.EDITOR);

        assertThrows(ForbiddenBoardAccessException.class, () -> cardService.createCard(columnId, "Title", "Desc", Priority.MEDIUM, null, Collections.emptyList(), userId));
    }

    @Test
    void updateCard_Forbidden_ThrowsException() {
        CardEntity card = new CardEntity();
        ColumnEntity column = new ColumnEntity();
        BoardEntity board = new BoardEntity();
        board.setId(UUID.randomUUID());
        column.setBoard(board);
        card.setColumn(column);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        doThrow(new ForbiddenBoardAccessException("Forbidden")).when(boardAccessService).requireAtLeast(board.getId(), userId, BoardRole.EDITOR);

        assertThrows(ForbiddenBoardAccessException.class, () -> cardService.updateCard(cardId, "Title", "Desc", Priority.MEDIUM, null, Collections.emptyList(), userId));
    }
}
