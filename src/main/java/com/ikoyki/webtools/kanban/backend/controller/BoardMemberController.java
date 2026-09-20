package com.ikoyki.webtools.kanban.backend.controller;

import com.ikoyki.webtools.kanban.backend.dto.request.*;
import com.ikoyki.webtools.kanban.backend.dto.response.BoardMemberResponse;
import com.ikoyki.webtools.kanban.backend.entity.BoardMemberEntity;
import com.ikoyki.webtools.kanban.backend.security.AuthUser;
import com.ikoyki.webtools.kanban.backend.service.BoardMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1/boards/{boardId}/members")
@RequiredArgsConstructor
public class BoardMemberController {
    private final BoardMemberService boardMemberService;

    @GetMapping
    public ResponseEntity<List<BoardMemberResponse>> listMembers(@PathVariable UUID boardId, @AuthUser UUID currentUserId) {
        List<BoardMemberEntity> members = boardMemberService.getMembers(boardId, currentUserId);
        // Mapping to response DTO would go here, for now returning entities or simplified responses
        return ResponseEntity.ok(members.stream().map(m -> BoardMemberResponse.builder()
                .userId(m.getUser().getId())
                .email(m.getUser().getEmail())
                .displayName(m.getUser().getDisplayName())
                .role(m.getRole())
                .build()).toList());
    }

    @PostMapping
    public ResponseEntity<BoardMemberEntity> addMember(@PathVariable UUID boardId, @AuthUser UUID currentUserId, @RequestBody BoardMemberRequest request) {
        return ResponseEntity.ok(boardMemberService.addMember(boardId, request, currentUserId));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<BoardMemberEntity> updateRole(@PathVariable UUID boardId, @PathVariable UUID userId, @AuthUser UUID currentUserId, @RequestBody UpdateMemberRoleRequest request) {
        return ResponseEntity.ok(boardMemberService.updateRole(boardId, userId, request, currentUserId));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID boardId, @PathVariable UUID userId, @AuthUser UUID currentUserId) {
        boardMemberService.removeMember(boardId, userId, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
