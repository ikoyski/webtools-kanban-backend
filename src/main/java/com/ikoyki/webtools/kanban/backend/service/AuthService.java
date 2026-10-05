package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.dto.request.ChangePasswordRequest;
import com.ikoyki.webtools.kanban.backend.dto.request.ForgotPasswordRequest;
import com.ikoyki.webtools.kanban.backend.dto.request.LoginRequest;
import com.ikoyki.webtools.kanban.backend.dto.request.RegisterRequest;
import com.ikoyki.webtools.kanban.backend.dto.request.ResetPasswordRequest;
import com.ikoyki.webtools.kanban.backend.dto.response.AuthResponse;
import com.ikoyki.webtools.kanban.backend.entity.UserEntity;
import com.ikoyki.webtools.kanban.backend.entity.UserProviderEntity;
import com.ikoyki.webtools.kanban.backend.entity.PasswordResetTokenEntity;
import com.ikoyki.webtools.kanban.backend.exception.BadRequestException;
import com.ikoyki.webtools.kanban.backend.exception.BadCredentialsException;
import com.ikoyki.webtools.kanban.backend.event.PasswordResetRequestedEvent;
import com.ikoyki.webtools.kanban.backend.repository.UserProviderRepository;
import com.ikoyki.webtools.kanban.backend.repository.UserRepository;
import com.ikoyki.webtools.kanban.backend.repository.PasswordResetTokenRepository;
import com.ikoyki.webtools.kanban.backend.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserProviderRepository providerRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder; // BCryptPasswordEncoder bean
    private final JwtTokenProvider jwtTokenProvider; // Your custom JWT generator
    private final TurnstileService turnstileService;
    private final ApplicationEventPublisher eventPublisher;

    // 1. TRADITIONAL SIGNUP
    @Transactional
    public AuthResponse registerLocal(RegisterRequest request, String remoteIp) {
        if (!turnstileService.verify(request.getTurnstileToken(), remoteIp)) {
            throw new BadRequestException("Invalid Turnstile token");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setDisplayName(request.getDisplayName());
        user = userRepository.save(user);

        UserProviderEntity localProvider = new UserProviderEntity();
        localProvider.setUser(user);
        localProvider.setProviderType("LOCAL");
        localProvider.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        providerRepository.save(localProvider);

        String jwt = jwtTokenProvider.generateToken(user);

        return AuthResponse.builder()
                .token(jwt).email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(null)
                .settings(Map.of("theme", "light"))
                .build();
    }

    // 2. TRADITIONAL LOGIN
    public AuthResponse loginLocal(LoginRequest request, String remoteIp) {
        if (!turnstileService.verify(request.getTurnstileToken(), remoteIp)) {
            throw new BadRequestException("Invalid Turnstile token");
        }

        UserProviderEntity provider = providerRepository.findByProviderTypeAndUserEmail("LOCAL", request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), provider.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String jwt = jwtTokenProvider.generateToken(provider.getUser());
        UserEntity user = provider.getUser();

        return AuthResponse.builder()
                .token(jwt)
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .settings(Map.of("theme", "light"))
                .build();
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        // Load the managed entity: query parameters must be persistent instances, not detached/blank ones.
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        UserProviderEntity provider = providerRepository.findByUserAndProviderType(user, "LOCAL")
                .orElseThrow(() -> new BadRequestException("Password change is only available for local accounts"));

        if (!passwordEncoder.matches(request.getOldPassword(), provider.getPasswordHash())) {
            throw new BadCredentialsException("Incorrect current password");
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BadRequestException("New password must be different from the old password");
        }

        provider.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        providerRepository.save(provider);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        if (!turnstileService.verify(request.getTurnstileToken(), "unknown")) {
            throw new BadRequestException("Invalid Turnstile token");
        }

        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            tokenRepository.deleteByUser(user);

            String token = UUID.randomUUID().toString();
            PasswordResetTokenEntity resetToken = PasswordResetTokenEntity.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(OffsetDateTime.now().plusHours(1))
                    .build();

            tokenRepository.save(resetToken);
            eventPublisher.publishEvent(new PasswordResetRequestedEvent(user.getEmail(), token));
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetTokenEntity resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new BadRequestException("Reset token has expired");
        }

        UserEntity user = resetToken.getUser();
        UserProviderEntity provider = providerRepository.findByUserAndProviderType(user, "LOCAL")
                .orElseThrow(() -> new BadRequestException("Password reset is only available for local accounts"));

        provider.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        providerRepository.save(provider);

        tokenRepository.delete(resetToken);
    }
}
