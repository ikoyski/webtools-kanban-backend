package com.ikoyki.webtools.kanban.backend.controller;

import com.ikoyki.webtools.kanban.backend.dto.request.ChangePasswordRequest;
import com.ikoyki.webtools.kanban.backend.dto.request.ForgotPasswordRequest;
import com.ikoyki.webtools.kanban.backend.dto.request.LoginRequest;
import com.ikoyki.webtools.kanban.backend.dto.request.RegisterRequest;
import com.ikoyki.webtools.kanban.backend.dto.request.ResetPasswordRequest;
import com.ikoyki.webtools.kanban.backend.dto.response.AuthResponse;
import com.ikoyki.webtools.kanban.backend.entity.UserEntity;
import com.ikoyki.webtools.kanban.backend.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signupLocal(@RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        String remoteIp = servletRequest.getRemoteAddr();
        return ResponseEntity.ok(authService.registerLocal(request, remoteIp));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginLocal(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String remoteIp = servletRequest.getRemoteAddr();
        return ResponseEntity.ok(authService.loginLocal(request, remoteIp));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthUser UserEntity user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(user, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}
