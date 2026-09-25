package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.dto.request.CommentRequest;
import com.ikoyki.webtools.kanban.backend.dto.response.CommentResponse;
import com.ikoyki.webtools.kanban.backend.entity.CardCommentEntity;
import com.ikoyki.webtools.kanban.backend.entity.BoardRole;
import com.ikoyki.webtools.kanban.backend.entity.CardEntity;
import com.ikoyki.webtools.kanban.backend.exception.ResourceNotFoundException;
import com.ikoyki.webtools.kanban.backend.exception.BadRequestException;
import com.ikoyki.webtools.kanban.backend.repository.CardCommentRepository;
import com.ikoyki.webtools.kanban.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CardCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BoardAccessService boardAccessService;
    private final com.ikoyki.webtools.kanban.backend.repository.CardRepository cardRepository;

    public List<CommentResponse> getCommentsForCard(UUID cardId, UUID userId) {
        CardEntity card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        boardAccessService.requireAtLeast(card.getColumn().getBoard().getId(), userId, BoardRole.VIEWER);

        return commentRepository.findByCardIdOrderByCreatedAtAsc(cardId).stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse addComment(UUID cardId, CommentRequest request, UUID userId) {
        CardEntity card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        boardAccessService.requireAtLeast(card.getColumn().getBoard().getId(), userId, BoardRole.EDITOR);

        CardCommentEntity comment = CardCommentEntity.builder().id(UUID.randomUUID()).cardId(cardId)
                .userId(userId).content(request.getContent()).createdAt(OffsetDateTime.now()).build();

        CardCommentEntity saved = commentRepository.save(comment);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        CardCommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        CardEntity card = cardRepository.findById(comment.getCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        // Only OWNER or the author can delete
        BoardRole role = boardAccessService.requireMembership(card.getColumn().getBoard().getId(), userId);
        if (role != BoardRole.OWNER && !comment.getUserId().equals(userId)) {
            throw new BadRequestException("Not authorized to delete this comment");
        }

        commentRepository.delete(comment);
    }

    private CommentResponse mapToResponse(CardCommentEntity entity) {
        var user = userRepository.findById(entity.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return CommentResponse.builder().id(entity.getId()).userId(entity.getUserId())
                .userName(user.getDisplayName()).content(entity.getContent())
                .createdAt(entity.getCreatedAt()).build();
    }
}
