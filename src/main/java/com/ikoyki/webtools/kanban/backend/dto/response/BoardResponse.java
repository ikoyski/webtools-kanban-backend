package com.ikoyki.webtools.kanban.backend.dto.response;

import com.ikoyki.webtools.kanban.backend.entity.BoardRole;

import lombok.*;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponse {
    private UUID id;
    private String name;
    private Map<UUID, ColumnResponse> columns;
    private Map<UUID, CardResponse> cards;
    private BoardRole role;
}
