package com.ikoyki.webtools.kanban.backend.dto.request;

import com.ikoyki.webtools.kanban.backend.entity.Priority;

import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCardRequest {
    private String title;
    // Rich-text HTML from the frontend's WYSIWYG editor; bounded to stop pathologically large payloads.
    @Size(max = 50_000)
    private String description;
    private Priority priority;
    private LocalDate dueDate;
    private List<String> labels;
}
