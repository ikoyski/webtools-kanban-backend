package com.ikoyki.webtools.kanban.backend.controller;

import com.ikoyki.webtools.kanban.backend.dto.request.LoginRequest;
import com.ikoyki.webtools.kanban.backend.dto.request.RegisterRequest;
import com.ikoyki.webtools.kanban.backend.dto.response.AuthResponse;
import com.ikoyki.webtools.kanban.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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
}
