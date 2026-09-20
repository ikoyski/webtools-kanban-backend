package com.ikoyki.webtools.kanban.backend.dto.request;

import lombok.*;
import com.ikoyki.webtools.kanban.backend.entity.BoardRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberRoleRequest {
    private BoardRole role;
}
