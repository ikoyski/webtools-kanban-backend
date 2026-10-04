package com.ikoyki.webtools.kanban.backend.repository;

import com.ikoyki.webtools.kanban.backend.entity.PasswordResetTokenEntity;
import com.ikoyki.webtools.kanban.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, java.util.UUID> {
    Optional<PasswordResetTokenEntity> findByToken(String token);
    void deleteByUser(UserEntity user);
}
