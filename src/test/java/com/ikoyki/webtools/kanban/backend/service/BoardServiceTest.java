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
class BoardServiceTest {

    @Mock private BoardRepository boardRepository;
    @Mock private ColumnRepository columnRepository;
    @Mock private CardRepository cardRepository;
    @Mock private BoardMemberRepository boardMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private BoardAccessService boardAccessService;

    @InjectMocks
    private BoardService boardService;

    private UUID boardId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        boardId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void createBoard_Success() {
        UserEntity user = new UserEntity();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(boardRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        BoardEntity result = boardService.createBoard("Test Board", userId);

        assertNotNull(result);
        assertEquals("Test Board", result.getName());
        verify(boardMemberRepository).save(any());
    }

    @Test
    void getBoard_Forbidden_ThrowsException() {
        doThrow(new ForbiddenBoardAccessException("Forbidden")).when(boardAccessService).requireMembership(boardId, userId);

        assertThrows(ForbiddenBoardAccessException.class, () -> boardService.getBoard(boardId, userId));
    }

    @Test
    void renameBoard_Forbidden_ThrowsException() {
        doThrow(new ForbiddenBoardAccessException("Forbidden")).when(boardAccessService).requireAtLeast(boardId, userId, BoardRole.EDITOR);

        assertThrows(ForbiddenBoardAccessException.class, () -> boardService.renameBoard(boardId, "New Name", userId));
    }

    @Test
    void deleteBoard_Forbidden_ThrowsException() {
        doThrow(new ForbiddenBoardAccessException("Forbidden")).when(boardAccessService).requireAtLeast(boardId, userId, BoardRole.OWNER);

        assertThrows(ForbiddenBoardAccessException.class, () -> boardService.deleteBoard(boardId, userId));
    }
}
