package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.dto.request.ImportBoardRequest;
import com.ikoyki.webtools.kanban.backend.dto.response.BoardListResponse;
import com.ikoyki.webtools.kanban.backend.entity.*;
import com.ikoyki.webtools.kanban.backend.exception.*;
import com.ikoyki.webtools.kanban.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;
    private final ColumnRepository columnRepository;
    private final CardRepository cardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final UserRepository userRepository;
    private final BoardAccessService boardAccessService;

    @Transactional(readOnly = true)
    public List<BoardListResponse> listBoardsForUser(UUID userId) {
        return boardMemberRepository.findAllByUserId(userId).stream()
                .map(member -> {
                    BoardEntity board = member.getBoard();
                    return BoardListResponse.builder()
                            .id(board.getId())
                            .name(board.getName())
                            .role(member.getRole())
                            .updatedAt(board.getCreatedAt()) // Using createdAt as surrogate for updatedAt in v1
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public BoardEntity createBoard(String name, UUID ownerId) {
        BoardEntity board = BoardEntity.builder()
                .name(name)
                .createdBy(ownerId)
                .build();
        BoardEntity savedBoard = boardRepository.save(board);

        BoardMemberEntity member = BoardMemberEntity.builder()
                .id(UUID.randomUUID())
                .board(savedBoard)
                .user(userRepository.findById(ownerId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")))
                .role(BoardRole.OWNER)
                .createdAt(java.time.OffsetDateTime.now())
                .build();
        boardMemberRepository.save(member);

        return savedBoard;
    }

    @Transactional(readOnly = true)
    public BoardEntity getBoard(UUID id, UUID userId) {
        boardAccessService.requireMembership(id, userId);
        BoardEntity board = boardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));

        // We must filter out archived cards when loading the board's data
        // Note: BoardEntity doesn't have a direct reference to cards,
        // so the filtration happens at the CardRepository level when the
        // frontend requests cards for columns.

        return board;
    }

    @Transactional
    public BoardEntity renameBoard(UUID id, String newName, UUID userId) {
        boardAccessService.requireAtLeast(id, userId, BoardRole.EDITOR);
        BoardEntity board = boardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        board.setName(newName);
        return boardRepository.save(board);
    }

    @Transactional
    public void deleteBoard(UUID id, UUID userId) {
        boardAccessService.requireAtLeast(id, userId, BoardRole.OWNER);
        BoardEntity board = boardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        boardRepository.delete(board);
    }

    @Transactional
    public BoardEntity importBoard(UUID boardId, ImportBoardRequest request, UUID userId) {
        boardAccessService.requireAtLeast(boardId, userId, BoardRole.EDITOR);
        if (request.getColumns() == null) {
            throw new InvalidImportException("Columns list cannot be null");
        }

        BoardEntity board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));

        cardRepository.deleteByColumn_Board_Id(boardId);
        columnRepository.deleteByBoardId(boardId);

        if (request.getName() != null) {
            board.setName(request.getName());
            boardRepository.save(board);
        }

        List<ColumnEntity> createdColumns = new ArrayList<>();
        request.getColumns().forEach((key, colImp) -> {
            if (colImp.getTitle() == null || colImp.getTitle().isBlank()) {
                throw new InvalidImportException("Column title is required");
            }

            ColumnEntity column = ColumnEntity.builder()
                    .board(board)
                    .title(colImp.getTitle())
                    .position(colImp.getPosition())
                    .build();
            createdColumns.add(columnRepository.save(column));

            if (colImp.getCardIds() != null) {
                for (int j = 0; j < colImp.getCardIds().size(); j++) {
                    ImportBoardRequest.CardImport cardImp = request.getCards().get(colImp.getCardIds().get(j));
                    if (cardImp != null) {
                        if (cardImp.getTitle() == null || cardImp.getTitle().isBlank()) {
                            throw new InvalidImportException("Card title is required");
                        }
                        CardEntity card = CardEntity.builder()
                                .column(column)
                                .title(cardImp.getTitle())
                                .description(cardImp.getDescription() != null ? cardImp.getDescription() : "")
                                .priority(cardImp.getPriority() != null ? cardImp.getPriority() : Priority.MEDIUM)
                                .dueDate(cardImp.getDueDate() != null ? LocalDate.parse(cardImp.getDueDate()) : null)
                                .labels(cardImp.getLabels() != null ? cardImp.getLabels() : Collections.emptyList())
                                .position(j)
                                .build();
                        cardRepository.save(card);
                    }
                }
            }
        });

        return board;
    }
}
