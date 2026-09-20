package com.ikoyki.webtools.kanban.backend.dto.request;

import lombok.*;
import java.util.UUID;
import com.ikoyki.webtools.kanban.backend.entity.BoardRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardMemberRequest {
    private String email;
    private BoardRole role;
}
