package com.ikoyki.webtools.kanban.backend.controller;

import com.ikoyki.webtools.kanban.backend.dto.request.*;
import com.ikoyki.webtools.kanban.backend.dto.response.*;
import com.ikoyki.webtools.kanban.backend.entity.*;
import com.ikoyki.webtools.kanban.backend.mapper.BoardMapper;
import com.ikoyki.webtools.kanban.backend.repository.BoardMemberRepository;
import com.ikoyki.webtools.kanban.backend.security.AuthUser;
import com.ikoyki.webtools.kanban.backend.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1/boards")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;
    private final BoardMapper boardMapper;
    private final BoardMemberRepository boardMemberRepository;

    @GetMapping
    public ResponseEntity<List<BoardListResponse>> listBoards(@AuthUser UUID currentUserId) {
        return ResponseEntity.ok(boardService.listBoardsForUser(currentUserId));
    }

    @PostMapping
    public ResponseEntity<BoardResponse> createBoard(@AuthUser UUID currentUserId,
            @RequestBody CreateBoardRequest request) {
        BoardEntity board = boardService.createBoard(request.getName(), currentUserId);
        BoardRole role = BoardRole.OWNER;
        return ResponseEntity.ok(boardMapper.toResponse(board, role));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponse> getBoard(@PathVariable UUID boardId, @AuthUser UUID currentUserId) {
        BoardEntity board = boardService.getBoard(boardId, currentUserId);
        BoardRole role = boardMemberRepository.findByBoardIdAndUserId(boardId, currentUserId)
                .map(BoardMemberEntity::getRole)
                .orElse(BoardRole.VIEWER);
        return ResponseEntity.ok(boardMapper.toResponse(board, role));
    }

    @GetMapping("/{boardId}/export")
    public ResponseEntity<BoardResponse> exportBoard(@PathVariable UUID boardId, @AuthUser UUID currentUserId) {
        BoardEntity board = boardService.getBoard(boardId, currentUserId);
        BoardRole role = boardMemberRepository.findByBoardIdAndUserId(boardId, currentUserId)
                .map(BoardMemberEntity::getRole)
                .orElse(BoardRole.VIEWER);
        return ResponseEntity.ok(boardMapper.toResponse(board, role));
    }

    @PutMapping("/{boardId}/import")
    public ResponseEntity<BoardResponse> importBoard(@PathVariable UUID boardId, @AuthUser UUID currentUserId,
            @RequestBody ImportBoardRequest request) {
        BoardEntity board = boardService.importBoard(boardId, request, currentUserId);
        BoardRole role = boardMemberRepository.findByBoardIdAndUserId(boardId, currentUserId)
                .map(BoardMemberEntity::getRole)
                .orElse(BoardRole.VIEWER);
        return ResponseEntity.ok(boardMapper.toResponse(board, role));
    }

    @PatchMapping("/{boardId}")
    public ResponseEntity<BoardResponse> renameBoard(@PathVariable UUID boardId, @AuthUser UUID currentUserId,
            @RequestBody UpdateBoardRequest request) {
        BoardEntity board = boardService.renameBoard(boardId, request.getName(), currentUserId);
        BoardRole role = boardMemberRepository.findByBoardIdAndUserId(boardId, currentUserId)
                .map(BoardMemberEntity::getRole)
                .orElse(BoardRole.VIEWER);
        return ResponseEntity.ok(boardMapper.toResponse(board, role));
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(@PathVariable UUID boardId, @AuthUser UUID currentUserId) {
        boardService.deleteBoard(boardId, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
