package com.ikoyki.webtools.kanban.backend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ikoyki.webtools.kanban.backend.dto.response.UserProfileResponse;
import com.ikoyki.webtools.kanban.backend.security.AuthUser;
import com.ikoyki.webtools.kanban.backend.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Profile of the caller (identified by X-User-Id), for the UI header and avatar. */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(@AuthUser UUID currentUserId) {
        return ResponseEntity.ok(userService.getProfile(currentUserId));
    }
}
