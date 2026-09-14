package com.ikoyki.webtools.kanban.backend.controller;

import com.ikoyki.webtools.kanban.backend.dto.request.ImportBoardRequest;
import com.ikoyki.webtools.kanban.backend.dto.response.BoardResponse;
import com.ikoyki.webtools.kanban.backend.entity.BoardEntity;
import com.ikoyki.webtools.kanban.backend.mapper.BoardMapper;
import com.ikoyki.webtools.kanban.backend.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/v1/board")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;
    private final BoardMapper boardMapper;

    //for now we only have one board, this will change later
    private final UUID FIRST_EVER_BOARD_ID = UUID.fromString("e36425fc-6c46-43f7-b263-809494502937");

    @GetMapping
    public ResponseEntity<BoardResponse> getBoard() {
        BoardEntity board = boardService.getBoard(FIRST_EVER_BOARD_ID);
        return ResponseEntity.ok(boardMapper.toResponse(board));
    }

    @GetMapping("/export")
    public ResponseEntity<BoardResponse> exportBoard() {
        BoardEntity board = boardService.getBoard(FIRST_EVER_BOARD_ID);
        return ResponseEntity.ok(boardMapper.toResponse(board));
    }

    @PutMapping("/import")
    public ResponseEntity<BoardResponse> importBoard(@RequestBody ImportBoardRequest request) {
        BoardEntity board = boardService.importBoard(FIRST_EVER_BOARD_ID, request);
        return ResponseEntity.ok(boardMapper.toResponse(board));
    }
}
