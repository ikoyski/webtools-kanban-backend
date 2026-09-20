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
class ColumnServiceTest {

    @Mock private BoardRepository boardRepository;
    @Mock private ColumnRepository columnRepository;
    @Mock private CardRepository cardRepository;
    @Mock private BoardAccessService boardAccessService;

    @InjectMocks
    private ColumnService columnService;

    private UUID boardId;
    private UUID columnId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        boardId = UUID.randomUUID();
        columnId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void createColumn_Forbidden_ThrowsException() {
        doThrow(new ForbiddenBoardAccessException("Forbidden")).when(boardAccessService).requireAtLeast(boardId, userId, BoardRole.EDITOR);

        assertThrows(ForbiddenBoardAccessException.class, () -> columnService.createColumn(boardId, "Title", userId));
    }

    @Test
    void renameColumn_Forbidden_ThrowsException() {
        ColumnEntity column = new ColumnEntity();
        BoardEntity board = new BoardEntity();
        board.setId(boardId);
        column.setBoard(board);

        when(columnRepository.findById(columnId)).thenReturn(Optional.of(column));
        doThrow(new ForbiddenBoardAccessException("Forbidden")).when(boardAccessService).requireAtLeast(boardId, userId, BoardRole.EDITOR);

        assertThrows(ForbiddenBoardAccessException.class, () -> columnService.renameColumn(columnId, "New Title", userId));
    }
}
