package com.ikoyki.webtools.kanban.backend.controller;

import com.ikoyki.webtools.kanban.backend.dto.request.CommentRequest;
import com.ikoyki.webtools.kanban.backend.dto.response.CommentResponse;
import com.ikoyki.webtools.kanban.backend.security.AuthUser;
import com.ikoyki.webtools.kanban.backend.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/card/{cardId}")
    public ResponseEntity<List<CommentResponse>> getComments(@AuthUser UUID currentUserId, @PathVariable UUID cardId) {
        return ResponseEntity.ok(commentService.getCommentsForCard(cardId, currentUserId));
    }

    @PostMapping("/card/{cardId}")
    public ResponseEntity<CommentResponse> addComment(@AuthUser UUID currentUserId, @PathVariable UUID cardId,
            @RequestBody CommentRequest request) {
        return ResponseEntity.ok(commentService.addComment(cardId, request, currentUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@AuthUser UUID currentUserId, @PathVariable UUID id) {
        commentService.deleteComment(id, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
