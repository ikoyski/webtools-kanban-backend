package com.ikoyki.webtools.kanban.backend.dto.request;

import com.ikoyki.webtools.kanban.backend.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardRequest {
    @NotNull
    private UUID columnId;
    @NotBlank
    private String title;
    // Rich-text HTML from the frontend's WYSIWYG editor; bounded to stop pathologically large payloads.
    @Size(max = 50_000)
    private String description;
    private Priority priority;
    private LocalDate dueDate;
    private List<String> labels;
}
