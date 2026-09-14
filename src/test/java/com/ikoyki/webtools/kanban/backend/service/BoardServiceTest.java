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
        BoardEntity board = BoardEntity.builder().id(1L).name("Test Board").build();
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        BoardEntity result = boardService.getBoard(1L);

        assertEquals("Test Board", result.getName());
    }

    @Test
    void importBoard_ValidRequest_ReplacesData() {
        // Arrange
        BoardEntity board = BoardEntity.builder().id(1L).name("Old Name").build();
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        ImportBoardRequest.ColumnImport colImp = ImportBoardRequest.ColumnImport.builder()
                .title("New Col")
                .cards(List.of(ImportBoardRequest.CardImport.builder().title("New Card").build()))
                .build();
        ImportBoardRequest request = ImportBoardRequest.builder()
                .name("New Name")
                .columns(List.of(colImp))
                .build();

        // Act
        BoardEntity result = boardService.importBoard(1L, request);

        // Assert
        assertEquals("New Name", result.getName());
        verify(cardRepository).deleteByColumn_Board_Id(1L);
        verify(columnRepository).deleteByBoardId(1L);
        verify(columnRepository).save(any());
        verify(cardRepository).save(any());
    }

    @Test
    void importBoard_InvalidRequest_ThrowsException() {
        // Arrange
        BoardEntity board = BoardEntity.builder().id(1L).build();
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        ImportBoardRequest request = ImportBoardRequest.builder()
                .columns(List.of(ImportBoardRequest.ColumnImport.builder().title("").build())) // Invalid title
                .build();

        // Act & Assert
        assertThrows(InvalidImportException.class, () -> boardService.importBoard(1L, request));
    }
}
