package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.dto.request.ImportBoardRequest;
import com.ikoyki.webtools.kanban.backend.entity.BoardEntity;
import com.ikoyki.webtools.kanban.backend.entity.CardEntity;
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
    public BoardEntity getBoard(UUID id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Board not found"));
    }

    @Transactional
    public BoardEntity importBoard(UUID boardId, ImportBoardRequest request) {
        if (request.getColumns() == null) {
            throw new InvalidImportException("Columns list cannot be null");
        }

        BoardEntity board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));

        // Transactionally replace all
        cardRepository.deleteByColumn_Board_Id(boardId);
        columnRepository.deleteByBoardId(boardId);

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

                    CardEntity card = CardEntity.builder()
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
