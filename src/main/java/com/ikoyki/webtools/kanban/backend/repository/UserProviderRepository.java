package com.ikoyki.webtools.kanban.backend.repository;

import com.ikoyki.webtools.kanban.backend.entity.UserProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface UserProviderRepository extends JpaRepository<UserProviderEntity, UUID> {
    // Used for OAuth Login
    Optional<UserProviderEntity> findByProviderTypeAndProviderId(String providerType, String providerId);

    // User for Traditional Local Login
    Optional<UserProviderEntity> findByProviderTypeAndUserEmail(String providerType, String email);
}