package com.ikoyki.webtools.kanban.backend.service;

import com.ikoyki.webtools.kanban.backend.dto.request.LoginRequest;
import com.ikoyki.webtools.kanban.backend.dto.request.RegisterRequest;
import com.ikoyki.webtools.kanban.backend.dto.response.AuthResponse;
import com.ikoyki.webtools.kanban.backend.entity.UserEntity;
import com.ikoyki.webtools.kanban.backend.entity.UserProviderEntity;
import com.ikoyki.webtools.kanban.backend.exception.BadRequestException;
import com.ikoyki.webtools.kanban.backend.exception.BadCredentialsException;
import com.ikoyki.webtools.kanban.backend.repository.UserProviderRepository;
import com.ikoyki.webtools.kanban.backend.repository.UserRepository;
import com.ikoyki.webtools.kanban.backend.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserProviderRepository providerRepository;
    private final PasswordEncoder passwordEncoder; // BCryptPasswordEncoder bean
    private final JwtTokenProvider jwtTokenProvider; // Your custom JWT generator
    private final TurnstileService turnstileService;

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
}
