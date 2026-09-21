package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.entity.BoardRole;
import com.ikoyki.webtools.kanban.backend.entity.BoardMemberEntity;
import com.ikoyki.webtools.kanban.backend.exception.ForbiddenBoardAccessException;
import com.ikoyki.webtools.kanban.backend.repository.BoardMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardAccessService {

    private final BoardMemberRepository boardMemberRepository;

    @Transactional(readOnly = true)
    public BoardRole requireMembership(UUID boardId, UUID userId) {
        return boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .map(BoardMemberEntity::getRole)
                .orElseThrow(() -> new ForbiddenBoardAccessException("You are not a member of this board"));
    }

    @Transactional(readOnly = true)
    public void requireAtLeast(UUID boardId, UUID userId, BoardRole minRole) {
        BoardRole userRole = requireMembership(boardId, userId);
        if (userRole.ordinal() < minRole.ordinal()) {
            throw new ForbiddenBoardAccessException(
                    String.format("Role %s is required, but you have %s", minRole, userRole));
        }
    }
}
