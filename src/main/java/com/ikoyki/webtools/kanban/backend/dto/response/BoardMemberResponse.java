package com.ikoyki.webtools.kanban.backend.dto.response;

import lombok.*;
import java.util.UUID;
import com.ikoyki.webtools.kanban.backend.entity.BoardRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardMemberResponse {
    private UUID userId;
    private String email;
    private String displayName;
    private BoardRole role;
}
