package com.ikoyki.webtools.kanban.backend.dto.request;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderColumnsRequest {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnPosition {
        private UUID id;
        private Integer position;
    }

    private List<ColumnPosition> columns;
}
