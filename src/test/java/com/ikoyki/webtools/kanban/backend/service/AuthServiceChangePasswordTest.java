package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.dto.request.ChangePasswordRequest;
import com.ikoyki.webtools.kanban.backend.entity.UserEntity;
import com.ikoyki.webtools.kanban.backend.entity.UserProviderEntity;
import com.ikoyki.webtools.kanban.backend.exception.BadCredentialsException;
import com.ikoyki.webtools.kanban.backend.exception.BadRequestException;
import com.ikoyki.webtools.kanban.backend.repository.UserProviderRepository;
import com.ikoyki.webtools.kanban.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceChangePasswordTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProviderRepository providerRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private UUID userId;
    private UserEntity user;
    private UserProviderEntity provider;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = UserEntity.builder().id(userId).email("me@example.com").build();
        provider = UserProviderEntity.builder().user(user).providerType("LOCAL").passwordHash("old-hash").build();
    }

    private ChangePasswordRequest request(String oldPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(oldPassword);
        request.setNewPassword(newPassword);
        return request;
    }

    @Test
    void changePassword_loadsUserFromIdAndUpdatesHash() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(providerRepository.findByUserAndProviderType(user, "LOCAL")).thenReturn(Optional.of(provider));
        when(passwordEncoder.matches("current-pass", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("brand-new-pass")).thenReturn("new-hash");

        authService.changePassword(userId, request("current-pass", "brand-new-pass"));

        assertEquals("new-hash", provider.getPasswordHash());
        verify(providerRepository).save(provider);
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsAndDoesNotSave() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(providerRepository.findByUserAndProviderType(user, "LOCAL")).thenReturn(Optional.of(provider));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> authService.changePassword(userId, request("wrong", "brand-new-pass")));
        verify(providerRepository, never()).save(any());
    }

    @Test
    void changePassword_sameAsOld_throwsBadRequest() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(providerRepository.findByUserAndProviderType(user, "LOCAL")).thenReturn(Optional.of(provider));
        when(passwordEncoder.matches("same-pass-1", "old-hash")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> authService.changePassword(userId, request("same-pass-1", "same-pass-1")));
        verify(providerRepository, never()).save(any());
    }

    @Test
    void changePassword_nonLocalAccount_throwsBadRequest() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(providerRepository.findByUserAndProviderType(user, "LOCAL")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> authService.changePassword(userId, request("whatever", "brand-new-pass")));
    }
}
