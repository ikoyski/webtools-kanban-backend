package com.ikoyki.webtools.kanban.backend.controller;

import com.ikoyki.webtools.kanban.backend.dto.request.ImportBoardEntityRequest;
import com.ikoyki.webtools.kanban.backend.dto.response.BoardEntityResponse;
import com.ikoyki.webtools.kanban.backend.entity.BoardEntityEntity;
import com.ikoyki.webtools.kanban.backend.mapper.BoardEntityMapper;
import com.ikoyki.webtools.kanban.backend.service.BoardEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/board")
@RequiredArgsConstructor
public class BoardEntityController {
    private final BoardEntityService boardService;
    private final BoardEntityMapper boardMapper;

    @GetMapping
    public ResponseEntity<BoardEntityResponse> getBoardEntity() {
        BoardEntity board = boardService.getBoardEntity(1L);
        return ResponseEntity.ok(boardMapper.toResponse(board));
    }

    @GetMapping("/export")
    public ResponseEntity<BoardEntityResponse> exportBoardEntity() {
        BoardEntity board = boardService.getBoardEntity(1L);
        return ResponseEntity.ok(boardMapper.toResponse(board));
    }

    @PutMapping("/import")
    public ResponseEntity<BoardEntityResponse> importBoardEntity(@RequestBody ImportBoardEntityRequest request) {
        BoardEntity board = boardService.importBoardEntity(1L, request);
        return ResponseEntity.ok(boardMapper.toResponse(board));
    }
}
