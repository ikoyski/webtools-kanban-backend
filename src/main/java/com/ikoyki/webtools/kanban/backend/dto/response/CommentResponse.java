package com.ikoyki.webtools.kanban.backend.dto.response;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private String content;
    private OffsetDateTime createdAt;
}
