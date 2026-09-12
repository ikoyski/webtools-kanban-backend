package com.ikoyki.webtools.kanban.backend.controller;

import com.ikoyki.webtools.kanban.backend.dto.request.*;
import com.ikoyki.webtools.kanban.backend.dto.response.ColumnResponse;
import com.ikoyki.webtools.kanban.backend.entity.ColumnEntity;
import com.ikoyki.webtools.kanban.backend.mapper.BoardMapper;
import com.ikoyki.webtools.kanban.backend.service.ColumnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/v1/columns")
@RequiredArgsConstructor
public class ColumnController {
    private final ColumnService columnService;
    private final BoardMapper boardMapper;

    @PostMapping
    public ResponseEntity<ColumnResponse> createColumn(@Valid @RequestBody CreateColumnRequest request) {
        ColumnEntity column = columnService.createColumn(1L, request.getTitle());
        return ResponseEntity.ok(boardMapper.toColumnResponse(column));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ColumnResponse> renameColumn(@PathVariable UUID id, @Valid @RequestBody UpdateColumnRequest request) {
        ColumnEntity column = columnService.renameColumn(id, request.getTitle());
        return ResponseEntity.ok(boardMapper.toColumnResponse(column));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteColumn(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID transferTo) {
        columnService.deleteColumn(id, transferTo);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorderColumns(@RequestBody ReorderColumnsRequest request) {
        columnService.reorderColumns(request.getColumns());
        return ResponseEntity.ok().build();
    }
}
