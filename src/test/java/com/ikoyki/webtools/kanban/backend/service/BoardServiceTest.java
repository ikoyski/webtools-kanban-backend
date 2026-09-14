package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.dto.request.ImportBoardRequest;
import com.ikoyki.webtools.kanban.backend.entity.BoardEntity;
import com.ikoyki.webtools.kanban.backend.exception.InvalidImportException;
import com.ikoyki.webtools.kanban.backend.repository.BoardRepository;
import com.ikoyki.webtools.kanban.backend.repository.CardRepository;
import com.ikoyki.webtools.kanban.backend.repository.ColumnRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private ColumnRepository columnRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private BoardService boardService;

    @Test
    void getBoard_Success() {
        UUID boardId = UUID.randomUUID();
        BoardEntity board = BoardEntity.builder().id(boardId).name("Test Board").build();
        when(boardRepository.findById(boardId)).thenReturn(Optional.of(board));

        BoardEntity result = boardService.getBoard(boardId);

        assertEquals("Test Board", result.getName());
    }

    @Test
    void importBoard_ValidRequest_ReplacesData() {
        // Arrange
        UUID boardId = UUID.randomUUID();
        BoardEntity board = BoardEntity.builder().id(UUID.randomUUID()).name("Old Name").build();
        when(boardRepository.findById(boardId)).thenReturn(Optional.of(board));

        ImportBoardRequest.ColumnImport colImp = ImportBoardRequest.ColumnImport.builder()
                .title("New Col")
                .cards(List.of(ImportBoardRequest.CardImport.builder().title("New Card").build()))
                .build();
        ImportBoardRequest request = ImportBoardRequest.builder()
                .name("New Name")
                .columns(List.of(colImp))
                .build();

        // Act
        BoardEntity result = boardService.importBoard(boardId, request);

        // Assert
        assertEquals("New Name", result.getName());
        verify(cardRepository).deleteByColumn_Board_Id(boardId);
        verify(columnRepository).deleteByBoardId(boardId);
        verify(columnRepository).save(any());
        verify(cardRepository).save(any());
    }

    @Test
    void importBoard_InvalidRequest_ThrowsException() {
        // Arrange
        UUID boardId = UUID.randomUUID();
        BoardEntity board = BoardEntity.builder().id(UUID.randomUUID()).build();
        when(boardRepository.findById(boardId)).thenReturn(Optional.of(board));

        ImportBoardRequest request = ImportBoardRequest.builder()
                .columns(List.of(ImportBoardRequest.ColumnImport.builder().title("").build())) // Invalid title
                .build();

        // Act & Assert
        assertThrows(InvalidImportException.class, () -> boardService.importBoard(boardId, request));
    }
}
