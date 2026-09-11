package com.ikoyki.webtools.kanban.backend.dto.request;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportBoardRequest {
    private String name;
    private List<ColumnImport> columns;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnImport {
        private String title;
        private List<CardImport> cards;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardImport {
        private String title;
        private String description;
        private String priority;
        private String dueDate; // ISO date string
        private List<String> labels;
    }
}
