package com.ikoyki.webtools.kanban.backend.controller;

import com.ikoyki.webtools.kanban.backend.dto.request.*;
import com.ikoyki.webtools.kanban.backend.dto.response.CardEntityResponse;
import com.ikoyki.webtools.kanban.backend.entity.CardEntityEntity;
import com.ikoyki.webtools.kanban.backend.mapper.BoardMapper;
import com.ikoyki.webtools.kanban.backend.service.CardEntityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardEntityController {
    private final CardEntityService cardService;
    private final BoardMapper boardMapper;

    @PostMapping
    public ResponseEntity<CardEntityResponse> createCardEntity(@Valid @RequestBody CreateCardEntityRequest request) {
        CardEntity card = cardService.createCardEntity(
                request.getColumnId(),
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.getDueDate(),
                request.getLabels()
        );
        return ResponseEntity.ok(boardMapper.toCardEntityResponse(card));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CardEntityResponse> updateCardEntity(@PathVariable UUID id, @RequestBody UpdateCardEntityRequest request) {
        CardEntity card = cardService.updateCardEntity(
                id,
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.getDueDate(),
                request.getLabels()
        );
        return ResponseEntity.ok(boardMapper.toCardEntityResponse(card));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCardEntity(@PathVariable UUID id) {
        cardService.deleteCardEntity(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<CardEntityResponse> moveCardEntity(@PathVariable UUID id, @Valid @RequestBody MoveCardEntityRequest request) {
        CardEntity card = cardService.moveCardEntity(id, request.getColumnId(), request.getPosition());
        return ResponseEntity.ok(boardMapper.toCardEntityResponse(card));
    }
}
