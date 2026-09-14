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

    @Transactional
    void deleteByColumn_Board_Id(Long boardId);
}
