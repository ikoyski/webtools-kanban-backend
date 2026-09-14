package com.ikoyki.webtools.kanban.backend.repository;

import com.ikoyki.webtools.kanban.backend.entity.BoardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<BoardEntity, Long> {
}
