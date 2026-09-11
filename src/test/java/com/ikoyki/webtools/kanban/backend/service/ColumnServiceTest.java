package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.entity.Card;
import com.ikoyki.webtools.kanban.backend.entity.ColumnEntity;
import com.ikoyki.webtools.kanban.backend.exception.ColumnNotEmptyException;
import com.ikoyki.webtools.kanban.backend.repository.CardRepository;
import com.ikoyki.webtools.kanban.backend.repository.ColumnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ColumnServiceTest {

    @Mock
    private ColumnRepository columnRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private ColumnService columnService;

    private UUID columnId;
    private UUID destColumnId;

    @BeforeEach
    void setUp() {
        columnId = UUID.randomUUID();
        destColumnId = UUID.randomUUID();
    }

    @Test
    void createColumn_SetsCorrectPosition() {
        // Arrange
        Long boardId = 1L;
        List<ColumnEntity> existing = List.of(new ColumnEntity(), new ColumnEntity());
        when(columnRepository.findByBoardIdOrderByPositionAsc(boardId)).thenReturn(existing);
        when(columnRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        // Act
        ColumnEntity result = columnService.createColumn(boardId, "New Column");

        // Assert
        assertEquals(2, result.getPosition());
        verify(columnRepository).save(any());
    }

    @Test
    void deleteColumn_Empty_Success() {
        // Arrange
        ColumnEntity col = new ColumnEntity();
        col.setId(columnId);
        when(columnRepository.findById(columnId)).thenReturn(Optional.of(col));
        when(cardRepository.findByColumnIdOrderByPositionAsc(columnId)).thenReturn(Collections.emptyList());

        // Act
        columnService.deleteColumn(columnId, null);

        // Assert
        verify(columnRepository).delete(col);
    }

    @Test
    void deleteColumn_NotEmpty_WithoutTransfer_ThrowsException() {
        // Arrange
        ColumnEntity col = new ColumnEntity();
        col.setId(columnId);
        when(columnRepository.findById(columnId)).thenReturn(Optional.of(col));
        when(cardRepository.findByColumnIdOrderByPositionAsc(columnId)).thenReturn(List.of(new Card()));

        // Act & Assert
        assertThrows(ColumnNotEmptyException.class, () -> columnService.deleteColumn(columnId, null));
        verify(columnRepository, never()).delete(any());
    }

    @Test
    void deleteColumn_NotEmpty_WithTransfer_MovesCardsAndDeletes() {
        // Arrange
        ColumnEntity col = new ColumnEntity();
        col.setId(columnId);
        ColumnEntity destCol = new ColumnEntity();
        destCol.setId(destColumnId);

        Card card1 = new Card();
        card1.setPosition(0);
        Card card2 = new Card();
        card2.setPosition(1);
        List<Card> cards = new ArrayList<>(List.of(card1, card2));

        when(columnRepository.findById(columnId)).thenReturn(Optional.of(col));
        when(columnRepository.findById(destColumnId)).thenReturn(Optional.of(destCol));
        when(cardRepository.findByColumnIdOrderByPositionAsc(columnId)).thenReturn(cards);
        when(cardRepository.findByColumnIdOrderByPositionAsc(destColumnId)).thenReturn(new ArrayList<>());

        // Act
        columnService.deleteColumn(columnId, destColumnId);

        // Assert
        assertEquals(destCol, card1.getColumn());
        assertEquals(destCol, card2.getColumn());
        assertEquals(0, card1.getPosition());
        assertEquals(1, card2.getPosition());
        verify(cardRepository).saveAll(any());
        verify(columnRepository).delete(col);
    }
}
