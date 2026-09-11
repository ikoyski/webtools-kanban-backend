package com.ikoyki.webtools.kanban.backend.dto.request;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCardRequest {
    private String title;
    private String description;
    private String priority;
    private LocalDate dueDate;
    private List<String> labels;
}
