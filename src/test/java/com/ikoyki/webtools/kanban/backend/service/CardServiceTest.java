package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.entity.Card;
import com.ikoyki.webtools.kanban.backend.entity.ColumnEntity;
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
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private ColumnRepository columnRepository;

    @InjectMocks
    private CardService cardService;

    private UUID columnAId;
    private UUID columnBId;
    private UUID card1Id;
    private UUID card2Id;
    private UUID card3Id;

    @BeforeEach
    void setUp() {
        columnAId = UUID.randomUUID();
        columnBId = UUID.randomUUID();
        card1Id = UUID.randomUUID();
        card2Id = UUID.randomUUID();
        card3Id = UUID.randomUUID();
    }

    @Test
    void moveCard_SameColumn_ReordersCorrectly() {
        // Arrange
        ColumnEntity colA = new ColumnEntity();
        colA.setId(columnAId);

        Card card1 = Card.builder().id(card1Id).column(colA).position(0).build();
        Card card2 = Card.builder().id(card2Id).column(colA).position(1).build();
        Card card3 = Card.builder().id(card3Id).column(colA).position(2).build();

        List<Card> cards = new ArrayList<>(List.of(card1, card2, card3));
        when(cardRepository.findById(card1Id)).thenReturn(Optional.of(card1));
        when(cardRepository.findByColumnIdOrderByPositionAsc(columnAId)).thenReturn(cards);
        when(cardRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        // Act: Move card1 (pos 0) to position 2
        Card result = cardService.moveCard(card1Id, columnAId, 2);

        // Assert
        assertEquals(2, result.getPosition());
        assertEquals(0, cards.get(0).getPosition()); // card2
        assertEquals(1, cards.get(1).getPosition()); // card3
        assertEquals(2, cards.get(2).getPosition()); // card1
        verify(cardRepository).saveAll(any());
    }

    @Test
    void moveCard_CrossColumn_ReordersBothColumns() {
        // Arrange
        ColumnEntity colA = new ColumnEntity();
        colA.setId(columnAId);
        ColumnEntity colB = new ColumnEntity();
        colB.setId(columnBId);

        Card card1 = Card.builder().id(card1Id).column(colA).position(0).build();
        Card card2 = Card.builder().id(card2Id).column(colA).position(1).build();
        Card card3 = Card.builder().id(card3Id).column(colB).position(0).build();

        List<Card> cardsA = new ArrayList<>(List.of(card1, card2));
        List<Card> cardsB = new ArrayList<>(List.of(card3));

        when(cardRepository.findById(card1Id)).thenReturn(Optional.of(card1));
        when(cardRepository.findByColumnIdOrderByPositionAsc(columnAId)).thenReturn(cardsA);
        when(cardRepository.findByColumnIdOrderByPositionAsc(columnBId)).thenReturn(cardsB);
        when(columnRepository.findById(columnBId)).thenReturn(Optional.of(colB));
        when(cardRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        // Act: Move card1 from A to B at position 0
        Card result = cardService.moveCard(card1Id, columnBId, 0);

        // Assert
        assertEquals(colB, result.getColumn());
        assertEquals(0, result.getPosition());
        assertEquals(0, cardsA.get(0).getPosition()); // card2 shifted to 0
        assertEquals(0, cardsB.get(0).getPosition()); // card1 inserted at 0
        assertEquals(1, cardsB.get(1).getPosition()); // card3 shifted to 1
        verify(cardRepository, times(2)).saveAll(any());
    }

    @Test
    void moveCard_NotFound_ThrowsException() {
        when(cardRepository.findById(card1Id)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> cardService.moveCard(card1Id, columnAId, 0));
    }
}
