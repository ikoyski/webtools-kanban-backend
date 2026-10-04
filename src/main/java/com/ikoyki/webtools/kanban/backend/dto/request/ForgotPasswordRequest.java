package com.ikoyki.webtools.kanban.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String turnstileToken;
}
