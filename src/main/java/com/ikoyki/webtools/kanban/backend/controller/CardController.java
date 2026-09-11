package com.ikoyki.webtools.kanban.backend.controller;

import com.ikoyki.webtools.kanban.backend.dto.request.*;
import com.ikoyki.webtools.kanban.backend.dto.response.CardResponse;
import com.ikoyki.webtools.kanban.backend.entity.Card;
import com.ikoyki.webtools.kanban.backend.mapper.BoardMapper;
import com.ikoyki.webtools.kanban.backend.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardService cardService;
    private final BoardMapper boardMapper;

    @PostMapping
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CreateCardRequest request) {
        Card card = cardService.createCard(
                request.getColumnId(),
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.getDueDate(),
                request.getLabels()
        );
        return ResponseEntity.ok(boardMapper.toCardResponse(card));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CardResponse> updateCard(@PathVariable UUID id, @RequestBody UpdateCardRequest request) {
        Card card = cardService.updateCard(
                id,
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.getDueDate(),
                request.getLabels()
        );
        return ResponseEntity.ok(boardMapper.toCardResponse(card));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable UUID id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<CardResponse> moveCard(@PathVariable UUID id, @Valid @RequestBody MoveCardRequest request) {
        Card card = cardService.moveCard(id, request.getColumnId(), request.getPosition());
        return ResponseEntity.ok(boardMapper.toCardResponse(card));
    }
}
