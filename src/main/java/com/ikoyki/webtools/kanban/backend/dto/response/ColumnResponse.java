package com.ikoyki.webtools.kanban.backend.dto.response;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnResponse {
    private UUID id;
    private String title;
    private Integer position;
    private List<UUID> cardIds;
}
