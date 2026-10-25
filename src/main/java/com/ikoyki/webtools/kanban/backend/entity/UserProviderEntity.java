package com.ikoyki.webtools.kanban.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "user_provider")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProviderEntity {
    @Id
    @Generated(GenerationTime.INSERT)
    @Column(updatable = false, insertable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private String providerType; // "GOOGLE" today; "MICROSOFT", "GITHUB", ... later

    @Column(nullable = false)
    private String providerId; // the provider's stable subject id ("sub"), never the email

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
