package com.ikoyki.webtools.kanban.backend.repository;

import com.ikoyki.webtools.kanban.backend.entity.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CardRepository extends JpaRepository<CardEntity, UUID> {
    List<CardEntity> findByColumnIdOrderByPositionAsc(UUID columnId);

    List<CardEntity> findByColumn_Board_IdAndArchivedFalse(UUID boardId);

    List<CardEntity> findByColumn_Board_IdAndArchivedTrue(UUID boardId);

    @Transactional
    void deleteByColumn_Board_Id(UUID boardId);
}
