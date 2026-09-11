package com.ikoyki.webtools.kanban.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateColumnRequest {
    @NotBlank
    private String title;
}
