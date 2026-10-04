package com.ikoyki.webtools.kanban.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "password_reset_token")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetTokenEntity {
    @Id
    @Generated(GenerationTime.INSERT)
    @Column(updatable = false, insertable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private OffsetDateTime expiryDate;

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(this.expiryDate);
    }
}
