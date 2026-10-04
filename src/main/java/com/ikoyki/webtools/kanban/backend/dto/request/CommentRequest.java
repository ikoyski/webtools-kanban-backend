package com.ikoyki.webtools.kanban.backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {
    // Rich-text HTML from the frontend's WYSIWYG editor; bounded to stop pathologically large payloads.
    @Size(max = 10_000)
    private String content;
}
