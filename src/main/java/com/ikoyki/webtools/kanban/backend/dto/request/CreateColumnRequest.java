package com.ikoyki.webtools.kanban.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateColumnRequest {

    @NotNull
    private UUID boardId;

    @NotBlank
    private String title;
}
