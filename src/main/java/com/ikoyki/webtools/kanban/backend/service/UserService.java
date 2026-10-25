package com.ikoyki.webtools.kanban.backend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ikoyki.webtools.kanban.backend.dto.response.UserProfileResponse;
import com.ikoyki.webtools.kanban.backend.exception.ResourceNotFoundException;
import com.ikoyki.webtools.kanban.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> new UserProfileResponse(u.getId().toString(), u.getEmail(), u.getDisplayName(), u.getAvatarUrl()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
