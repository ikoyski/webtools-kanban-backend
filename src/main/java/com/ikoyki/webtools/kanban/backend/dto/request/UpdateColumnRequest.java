package com.ikoyki.webtools.kanban.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateColumnRequest {
    @NotBlank
    private String title;
}
