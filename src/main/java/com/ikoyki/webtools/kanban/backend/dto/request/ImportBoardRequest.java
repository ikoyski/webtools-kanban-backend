package com.ikoyki.webtools.kanban.backend.dto.request;

import lombok.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportBoardRequest {
    private UUID id;
    private String name;
    private Map<UUID, ColumnImport> columns;
    private Map<UUID, CardImport> cards;
    private Map<String, Object> settings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnImport {
        private UUID id;
        private String title;
        private Integer position;
        private List<UUID> cardIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardImport {
        private UUID id;
        private String title;
        private String description;
        private String priority;
        private String dueDate; // ISO date string
        private List<String> labels;
        private Integer position;
    }
}
