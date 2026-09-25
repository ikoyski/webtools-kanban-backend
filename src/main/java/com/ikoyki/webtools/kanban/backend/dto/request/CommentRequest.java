package com.ikoyki.webtools.kanban.backend.dto.request;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {
    private String content;
}
