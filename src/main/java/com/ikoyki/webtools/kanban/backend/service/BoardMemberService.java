package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.dto.request.*;
import com.ikoyki.webtools.kanban.backend.entity.*;
import com.ikoyki.webtools.kanban.backend.exception.*;
import com.ikoyki.webtools.kanban.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class BoardMemberService {
    private final BoardMemberRepository boardMemberRepository;
    private final UserRepository userRepository;
    private final BoardAccessService boardAccessService;

    @Transactional(readOnly = true)
    public List<BoardMemberEntity> getMembers(UUID boardId, UUID userId) {
        boardAccessService.requireMembership(boardId, userId);
        return boardMemberRepository.findAllByBoardId(boardId); // Needs repository method
    }

    @Transactional
    public BoardMemberEntity addMember(UUID boardId, BoardMemberRequest request, UUID userId) {
        boardAccessService.requireAtLeast(boardId, userId, BoardRole.OWNER);

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        if (boardMemberRepository.existsByBoardIdAndUserId(boardId, user.getId())) {
            throw new BadRequestException("User is already a member of this board");
        }

        BoardMemberEntity member = BoardMemberEntity.builder()
                .id(UUID.randomUUID())
                .board(boardMemberRepository.findByBoardIdAndUserId(boardId, userId).get().getBoard()) // Simplified
                .user(user)
                .role(request.getRole())
                .createdAt(java.time.OffsetDateTime.now())
                .build();

        return boardMemberRepository.save(member);
    }

    @Transactional
    public BoardMemberEntity updateRole(UUID boardId, UUID userIdToUpdate, UpdateMemberRoleRequest request,
            UUID actorId) {
        boardAccessService.requireAtLeast(boardId, actorId, BoardRole.OWNER);

        BoardMemberEntity member = boardMemberRepository.findByBoardIdAndUserId(boardId, userIdToUpdate)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (member.getRole() == BoardRole.OWNER && request.getRole() != BoardRole.OWNER) {
            if (boardMemberRepository.countByBoardIdAndRole(boardId, BoardRole.OWNER) <= 1) {
                throw new BadRequestException("Cannot demote the last owner of the board");
            }
        }

        member.setRole(request.getRole());
        return boardMemberRepository.save(member);
    }

    @Transactional
    public void removeMember(UUID boardId, UUID userIdToRemove, UUID actorId) {
        BoardMemberEntity member = boardMemberRepository.findByBoardIdAndUserId(boardId, userIdToRemove)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (!userIdToRemove.equals(actorId)) {
            boardAccessService.requireAtLeast(boardId, actorId, BoardRole.OWNER);
        }

        if (member.getRole() == BoardRole.OWNER
                && boardMemberRepository.countByBoardIdAndRole(boardId, BoardRole.OWNER) <= 1) {
            throw new BadRequestException("Cannot remove the last owner of the board");
        }

        boardMemberRepository.delete(member);
    }
}
