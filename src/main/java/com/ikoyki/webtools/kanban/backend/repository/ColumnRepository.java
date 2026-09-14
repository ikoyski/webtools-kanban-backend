package com.ikoyki.webtools.kanban.backend.repository;

import com.ikoyki.webtools.kanban.backend.entity.ColumnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ColumnRepository extends JpaRepository<ColumnEntity, UUID> {
    List<ColumnEntity> findByBoardIdOrderByPositionAsc(UUID boardId);

    @Transactional
    void deleteByBoardId(UUID boardId);
}
