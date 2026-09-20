package com.ikoyki.webtools.kanban.backend.dto.response;

import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import com.ikoyki.webtools.kanban.backend.entity.BoardRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardListResponse {
    private UUID id;
    private String name;
    private BoardRole role;
    private OffsetDateTime updatedAt;
}
