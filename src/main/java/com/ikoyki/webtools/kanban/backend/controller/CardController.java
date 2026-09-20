package com.ikoyki.webtools.kanban.backend.controller;

import com.ikoyki.webtools.kanban.backend.dto.request.*;
import com.ikoyki.webtools.kanban.backend.dto.response.CardResponse;
import com.ikoyki.webtools.kanban.backend.entity.CardEntity;
import com.ikoyki.webtools.kanban.backend.mapper.BoardMapper;
import com.ikoyki.webtools.kanban.backend.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/v1/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardService cardService;
    private final BoardMapper boardMapper;

    @PostMapping
    public ResponseEntity<CardResponse> createCardEntity(@AuthUser UUID currentUserId, @Valid @RequestBody CreateCardRequest request) {
        CardEntity card = cardService.createCard(
                request.getColumnId(),
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.getDueDate(),
                request.getLabels(),
                currentUserId);
        return ResponseEntity.ok(boardMapper.toCardResponse(card));
    }



    @PatchMapping("/{id}")
    public ResponseEntity<CardResponse> updateCardEntity(@AuthUser UUID currentUserId, @PathVariable UUID id, @RequestBody UpdateCardRequest request) {
        CardEntity card = cardService.updateCard(
                id,
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.getDueDate(),
                request.getLabels(),
                currentUserId);
        return ResponseEntity.ok(boardMapper.toCardResponse(card));
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@AuthUser UUID currentUserId, @PathVariable UUID id) {
        cardService.deleteCard(id, currentUserId);
        return ResponseEntity.noContent().build();
    }



    @PatchMapping("/{id}/move")
    public ResponseEntity<CardResponse> moveCardEntity(@AuthUser UUID currentUserId, @PathVariable UUID id,
            @Valid @RequestBody MoveCardRequest request) {
        CardEntity card = cardService.moveCard(id, request.getColumnId(), request.getPosition(), currentUserId);
        return ResponseEntity.ok(boardMapper.toCardResponse(card));
    }


}
