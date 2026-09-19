package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.entity.CardEntity;
import com.ikoyki.webtools.kanban.backend.entity.ColumnEntity;
import com.ikoyki.webtools.kanban.backend.repository.CardRepository;
import com.ikoyki.webtools.kanban.backend.repository.ColumnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;
    private final ColumnRepository columnRepository;

    @Transactional
    public CardEntity createCard(UUID columnId, String title, String description, String priority,
            java.time.LocalDate dueDate, List<String> labels) {
        ColumnEntity column = columnRepository.findById(columnId)
                .orElseThrow(() -> new RuntimeException("Column not found"));

        List<CardEntity> siblings = cardRepository.findByColumnIdOrderByPositionAsc(columnId);
        int position = siblings.size();

        CardEntity card = CardEntity.builder()
                .column(column)
                .title(title)
                .description(description != null ? description : "")
                .priority(priority != null ? priority : "Medium")
                .dueDate(dueDate)
                .labels(labels != null ? labels : Collections.emptyList())
                .position(position)
                .build();

        return cardRepository.save(card);
    }

    @Transactional
    public CardEntity updateCard(UUID id, String title, String description, String priority,
            java.time.LocalDate dueDate, List<String> labels) {
        CardEntity card = cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CardEntity not found"));

        if (title != null)
            card.setTitle(title);
        if (description != null)
            card.setDescription(description);
        if (priority != null)
            card.setPriority(priority);
        if (dueDate != null)
            card.setDueDate(dueDate);
        if (labels != null)
            card.setLabels(labels);

        return cardRepository.save(card);
    }

    @Transactional
    public void deleteCard(UUID id) {
        CardEntity card = cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CardEntity not found"));

        UUID columnId = card.getColumn().getId();
        cardRepository.delete(card);
        reindexCards(columnId);
    }

    @Transactional
    public CardEntity moveCard(UUID cardId, UUID destColumnId, Integer destPosition) {
        CardEntity card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("CardEntity not found"));

        UUID sourceColumnId = card.getColumn().getId();

        if (sourceColumnId.equals(destColumnId)) {
            List<CardEntity> siblings = cardRepository.findByColumnIdOrderByPositionAsc(sourceColumnId);
            siblings.remove(card);

            // Ensure position is within bounds
            int pos = Math.min(destPosition, siblings.size());
            siblings.add(pos, card);

            reassignPositions(siblings);
        } else {
            // Remove from source
            List<CardEntity> sourceSiblings = cardRepository.findByColumnIdOrderByPositionAsc(sourceColumnId);
            sourceSiblings.remove(card);
            reassignPositions(sourceSiblings);

            // Insert into destination
            ColumnEntity destColumn = columnRepository.findById(destColumnId)
                    .orElseThrow(() -> new RuntimeException("Destination column not found"));

            List<CardEntity> destSiblings = cardRepository.findByColumnIdOrderByPositionAsc(destColumnId);
            int pos = Math.min(destPosition, destSiblings.size());

            card.setColumn(destColumn);
            destSiblings.add(pos, card);
            reassignPositions(destSiblings);
        }

        return cardRepository.save(card);
    }

    private void reindexCards(UUID columnId) {
        List<CardEntity> siblings = cardRepository.findByColumnIdOrderByPositionAsc(columnId);
        reassignPositions(siblings);
    }

    private void reassignPositions(List<CardEntity> cards) {
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).setPosition(i);
        }
        cardRepository.saveAll(cards);
    }
}
