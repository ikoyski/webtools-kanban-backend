package com.ikoyki.webtools.kanban.backend.controller;

import com.ikoyki.webtools.kanban.backend.dto.request.ImportBoardRequest;
import com.ikoyki.webtools.kanban.backend.dto.response.BoardResponse;
import com.ikoyki.webtools.kanban.backend.entity.Board;
import com.ikoyki.webtools.kanban.backend.mapper.BoardMapper;
import com.ikoyki.webtools.kanban.backend.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/board")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;
    private final BoardMapper boardMapper;

    @GetMapping
    public ResponseEntity<BoardResponse> getBoard() {
        Board board = boardService.getBoard(1L);
        return ResponseEntity.ok(boardMapper.toResponse(board));
    }

    @GetMapping("/export")
    public ResponseEntity<BoardResponse> exportBoard() {
        Board board = boardService.getBoard(1L);
        return ResponseEntity.ok(boardMapper.toResponse(board));
    }

    @PutMapping("/import")
    public ResponseEntity<BoardResponse> importBoard(@RequestBody ImportBoardRequest request) {
        Board board = boardService.importBoard(1L, request);
        return ResponseEntity.ok(boardMapper.toResponse(board));
    }
}
