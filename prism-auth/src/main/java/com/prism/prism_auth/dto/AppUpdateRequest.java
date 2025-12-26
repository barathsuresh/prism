package com.prism.prism_auth.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating an App.
 * 
 * All fields are optional for partial updates.
 * Only provided fields will be updated, null/missing fields keep existing
 * values.
 * 
 * Validation rules:
 * - name: optional, 1-100 characters if provided
 * - slug: optional, 3-50 characters if provided, URL-safe
 * - description: optional, max 500 characters
 * - webhookUrl: optional, max 2083 characters (max URL length)
 * - allowedOrigins: optional list of CORS origins
 * - customMetadata: optional key-value metadata
 * - forceRegenerateSlug: optional, forces slug regeneration even if name
 * unchanged
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUpdateRequest {

    @Size(min = 1, max = 100, message = "App name must be between 1-100 characters")
    private String name;

    @Size(min = 3, max = 50, message = "App slug must be between 3-50 characters")
    private String slug; // Optional - can provide custom slug

    @Size(max = 500, message = "Description must be max 500 characters")
    private String description;

    @Size(max = 2083, message = "Webhook URL must be max 2083 characters")
    private String webhookUrl;

    private List<String> allowedOrigins;

    private Map<String, Object> customMetadata;

    @Builder.Default
    private Boolean forceRegenerateSlug = false;
}
