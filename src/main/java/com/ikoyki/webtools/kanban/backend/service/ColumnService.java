package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.dto.request.ReorderColumnsRequest;
import com.ikoyki.webtools.kanban.backend.entity.Card;
import com.ikoyki.webtools.kanban.backend.entity.ColumnEntity;
import com.ikoyki.webtools.kanban.backend.exception.ColumnNotEmptyException;
import com.ikoyki.webtools.kanban.backend.repository.CardRepository;
import com.ikoyki.webtools.kanban.backend.repository.ColumnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ColumnService {
    private final ColumnRepository columnRepository;
    private final CardRepository cardRepository;

    @Transactional
    public ColumnEntity createColumn(Long boardId, String title) {
        List<ColumnEntity> existing = columnRepository.findByBoardIdOrderByPositionAsc(boardId);
        int position = existing.size();

        // We need a Board object for the relation
        // For v1 we assume board 1 exists
        com.ikoyki.webtools.kanban.backend.entity.Board board = new com.ikoyki.webtools.kanban.backend.entity.Board();
        board.setId(boardId);

        ColumnEntity column = ColumnEntity.builder()
                .board(board)
                .title(title)
                .position(position)
                .build();

        return columnRepository.save(column);
    }

    @Transactional
    public ColumnEntity renameColumn(UUID id, String newTitle) {
        ColumnEntity column = columnRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Column not found"));
        column.setTitle(newTitle);
        return columnRepository.save(column);
    }

    @Transactional
    public void reorderColumns(List<ReorderColumnsRequest.ColumnPosition> updates) {
        List<ColumnEntity> columns = new ArrayList<>();
        for (ReorderColumnsRequest.ColumnPosition update : updates) {
            ColumnEntity column = columnRepository.findById(update.getId())
                    .orElseThrow(() -> new RuntimeException("Column not found: " + update.getId()));
            column.setPosition(update.getPosition());
            columns.add(column);
        }
        columnRepository.saveAll(columns);
    }

    @Transactional
    public void deleteColumn(UUID columnId, UUID transferToId) {
        ColumnEntity column = columnRepository.findById(columnId)
                .orElseThrow(() -> new RuntimeException("Column not found"));

        List<Card> cards = cardRepository.findByColumnIdOrderByPositionAsc(columnId);

        if (!cards.isEmpty()) {
            if (transferToId == null) {
                throw new ColumnNotEmptyException(cards.size());
            }
            if (transferToId.equals(columnId)) {
                throw new IllegalArgumentException("Cannot transfer cards to the same column");
            }

            ColumnEntity destColumn = columnRepository.findById(transferToId)
                    .orElseThrow(() -> new RuntimeException("Destination column not found"));

            List<Card> destSiblings = cardRepository.findByColumnIdOrderByPositionAsc(transferToId);

            for (Card card : cards) {
                card.setColumn(destColumn);
                destSiblings.add(card);
            }

            // Re-index destination
            for (int i = 0; i < destSiblings.size(); i++) {
                destSiblings.get(i).setPosition(i);
            }
            cardRepository.saveAll(destSiblings);
        }

        columnRepository.delete(column);
    }
}
