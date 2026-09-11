package com.ikoyki.webtools.kanban.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveCardRequest {
    @NotNull
    private UUID columnId;
    @NotNull
    private Integer position;
}
