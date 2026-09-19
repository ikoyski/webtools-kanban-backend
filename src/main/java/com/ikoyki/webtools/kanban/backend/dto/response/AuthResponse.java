package com.ikoyki.webtools.kanban.backend.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private String displayName;
    private String avatarUrl;
}
