package com.ikoyki.webtools.kanban.backend.controller;

import com.ikoyki.webtools.kanban.backend.dto.request.CommentRequest;
import com.ikoyki.webtools.kanban.backend.dto.response.CommentResponse;
import com.ikoyki.webtools.kanban.backend.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/v1/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/card/{cardId}")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable UUID cardId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(commentService.getCommentsForCard(cardId, userId));
    }

    @PostMapping("/card/{cardId}")
    public ResponseEntity<CommentResponse> addComment(@PathVariable UUID cardId, @RequestBody CommentRequest request, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        return ResponseEntity.ok(commentService.addComment(cardId, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        commentService.deleteComment(id, userId);
        return ResponseEntity.noContent().build();
    }
}
