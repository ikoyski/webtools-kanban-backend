package com.ikoyki.webtools.kanban.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank
    @NotNull 
    private String email;

    @NotBlank
    @NotNull 
    private String password;

    @NotBlank
    @NotNull 
    private String displayName;
}
