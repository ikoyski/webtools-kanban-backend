package com.ikoyki.webtools.kanban.backend.dto.response;

import com.ikoyki.webtools.kanban.backend.entity.Priority;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {
    private UUID id;
    private String title;
    private String description;
    private Priority priority;
    private LocalDate dueDate;
    private List<String> labels;
    private Integer position;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
