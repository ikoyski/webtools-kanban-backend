package com.ikoyki.webtools.kanban.backend.repository;

import com.ikoyki.webtools.kanban.backend.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
}
