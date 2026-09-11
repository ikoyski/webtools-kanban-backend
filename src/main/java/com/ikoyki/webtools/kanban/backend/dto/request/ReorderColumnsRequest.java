package com.ikoyki.webtools.kanban.backend.dto.request;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderColumnsRequest {
    public static class ColumnPosition {
        private UUID id;
        private Integer position;

        public ColumnPosition() {}
        public ColumnPosition(UUID id, Integer position) {
            this.id = id;
            this.position = position;
        }
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public Integer getPosition() { return position; }
        public void setPosition(Integer position) { this.position = position; }
    }

    private List<ColumnPosition> columns;
}
