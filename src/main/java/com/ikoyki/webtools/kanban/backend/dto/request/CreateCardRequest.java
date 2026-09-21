package com.ikoyki.webtools.kanban.backend.dto.request;

import com.ikoyki.webtools.kanban.backend.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private String description;
    private Priority priority;
    private LocalDate dueDate;
    private List<String> labels;
}
