package com.prism.prism_auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating an App.
 * 
 * Validation rules:
 * - name: required, 1-100 characters
 * - slug: required, 3-50 characters, URL-safe
 * - description: optional, max 500 characters
 * - webhookUrl: optional, valid URL format
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppRequest {

    @NotBlank(message = "App name is required")
    @Size(min = 1, max = 100, message = "App name must be between 1-100 characters")
    private String name;

    @NotBlank(message = "App slug is required")
    @Size(min = 3, max = 50, message = "App slug must be between 3-50 characters")
    private String slug;

    @Size(max = 500, message = "Description must be max 500 characters")
    private String description;

    @Size(max = 2083, message = "Webhook URL must be max 2083 characters")
    private String webhookUrl;
}
