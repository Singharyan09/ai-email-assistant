package com.example.emailassistant.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailRequest(
        String subject,
        @NotBlank(message = "Email body must not be empty") String body,
        String tone) {
}
