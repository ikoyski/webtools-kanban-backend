package com.ikoyki.webtools.kanban.backend.repository;

import com.ikoyki.webtools.kanban.backend.entity.BoardMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardMemberRepository extends JpaRepository<BoardMemberEntity, UUID> {
    Optional<BoardMemberEntity> findByBoardIdAndUserId(UUID boardId, UUID userId);
    List<BoardMemberEntity> findAllByUserId(UUID userId);
    List<BoardMemberEntity> findAllByBoardId(UUID boardId);
    boolean existsByBoardIdAndUserId(UUID boardId, UUID userId);

    long countByBoardIdAndRole(UUID boardId, com.ikoyki.webtools.kanban.backend.entity.BoardRole role);
}
