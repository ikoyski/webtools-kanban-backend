package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.entity.*;
import com.ikoyki.webtools.kanban.backend.exception.ForbiddenBoardAccessException;
import com.ikoyki.webtools.kanban.backend.repository.BoardMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardAccessServiceTest {

    @Mock
    private BoardMemberRepository boardMemberRepository;

    @InjectMocks
    private BoardAccessService boardAccessService;

    private UUID boardId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        boardId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void requireMembership_UserIsMember_ReturnsRole() {
        BoardMemberEntity member = BoardMemberEntity.builder().role(BoardRole.EDITOR).build();
        when(boardMemberRepository.findByBoardIdAndUserId(boardId, userId)).thenReturn(Optional.of(member));

        BoardRole role = boardAccessService.requireMembership(boardId, userId);

        assertEquals(BoardRole.EDITOR, role);
    }

    @Test
    void requireMembership_UserNotMember_ThrowsForbidden() {
        when(boardMemberRepository.findByBoardIdAndUserId(boardId, userId)).thenReturn(Optional.empty());

        assertThrows(ForbiddenBoardAccessException.class, () -> boardAccessService.requireMembership(boardId, userId));
    }

    @Test
    void requireAtLeast_UserHasHigherRole_Success() {
        BoardMemberEntity member = BoardMemberEntity.builder().role(BoardRole.OWNER).build();
        when(boardMemberRepository.findByBoardIdAndUserId(boardId, userId)).thenReturn(Optional.of(member));

        assertDoesNotThrow(() -> boardAccessService.requireAtLeast(boardId, userId, BoardRole.EDITOR));
    }

    @Test
    void requireAtLeast_UserHasExactRole_Success() {
        BoardMemberEntity member = BoardMemberEntity.builder().role(BoardRole.EDITOR).build();
        when(boardMemberRepository.findByBoardIdAndUserId(boardId, userId)).thenReturn(Optional.of(member));

        assertDoesNotThrow(() -> boardAccessService.requireAtLeast(boardId, userId, BoardRole.EDITOR));
    }

    @Test
    void requireAtLeast_UserHasLowerRole_ThrowsForbidden() {
        BoardMemberEntity member = BoardMemberEntity.builder().role(BoardRole.VIEWER).build();
        when(boardMemberRepository.findByBoardIdAndUserId(boardId, userId)).thenReturn(Optional.of(member));

        assertThrows(ForbiddenBoardAccessException.class, () -> boardAccessService.requireAtLeast(boardId, userId, BoardRole.EDITOR));
    }
}
