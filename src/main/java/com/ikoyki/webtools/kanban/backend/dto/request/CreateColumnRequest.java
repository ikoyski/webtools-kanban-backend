package com.ikoyki.webtools.kanban.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateColumnRequest {

    @NotBlank
    private UUID boardId;

    @NotBlank
    private String title;
}
