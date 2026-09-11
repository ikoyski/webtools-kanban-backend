package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.dto.request.ImportBoardRequest;
import com.ikoyki.webtools.kanban.backend.entity.Board;
import com.ikoyki.webtools.kanban.backend.entity.Card;
import com.ikoyki.webtools.kanban.backend.entity.ColumnEntity;
import com.ikoyki.webtools.kanban.backend.exception.InvalidImportException;
import com.ikoyki.webtools.kanban.backend.repository.BoardRepository;
import com.ikoyki.webtools.kanban.backend.repository.CardRepository;
import com.ikoyki.webtools.kanban.backend.repository.ColumnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;
    private final ColumnRepository columnRepository;
    private final CardRepository cardRepository;

    @Transactional(readOnly = true)
    public Board getBoard(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Board not found"));
    }

    @Transactional
    public Board importBoard(Long boardId, ImportBoardRequest request) {
        if (request.getColumns() == null) {
            throw new InvalidImportException("Columns list cannot be null");
        }

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));

        // Transactionally replace all
        cardRepository.deleteByBoardId(boardId); // Need to add this method to CardRepository
        columnRepository.deleteByBoardId(boardId); // Need to add this method to ColumnRepository

        if (request.getName() != null) {
            board.setName(request.getName());
            boardRepository.save(board);
        }

        List<ColumnEntity> createdColumns = new ArrayList<>();
        for (int i = 0; i < request.getColumns().size(); i++) {
            ImportBoardRequest.ColumnImport colImp = request.getColumns().get(i);
            if (colImp.getTitle() == null || colImp.getTitle().isBlank()) {
                throw new InvalidImportException("Column title is required");
            }

            ColumnEntity column = ColumnEntity.builder()
                    .board(board)
                    .title(colImp.getTitle())
                    .position(i)
                    .build();
            createdColumns.add(columnRepository.save(column));

            if (colImp.getCards() != null) {
                for (int j = 0; j < colImp.getCards().size(); j++) {
                    ImportBoardRequest.CardImport cardImp = colImp.getCards().get(j);
                    if (cardImp.getTitle() == null || cardImp.getTitle().isBlank()) {
                        throw new InvalidImportException("Card title is required");
                    }

                    Card card = Card.builder()
                            .column(column)
                            .title(cardImp.getTitle())
                            .description(cardImp.getDescription() != null ? cardImp.getDescription() : "")
                            .priority(cardImp.getPriority() != null ? cardImp.getPriority() : "Medium")
                            .dueDate(cardImp.getDueDate() != null ? LocalDate.parse(cardImp.getDueDate()) : null)
                            .labels(cardImp.getLabels() != null ? cardImp.getLabels() : Collections.emptyList())
                            .position(j)
                            .build();
                    cardRepository.save(card);
                }
            }
        }

        return board;
    }
}
