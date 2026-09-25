package com.ikoyki.webtools.kanban.backend.repository;

import com.ikoyki.webtools.kanban.backend.entity.CardCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CardCommentRepository extends JpaRepository<CardCommentEntity, UUID> {
    List<CardCommentEntity> findByCardIdOrderByCreatedAtAsc(UUID cardId);
}
