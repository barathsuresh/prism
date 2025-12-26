package com.prism.prism_auth.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for app response.
 * 
 * Used when returning app information to clients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppResponse {

    /**
     * App ID (MongoDB ObjectId)
     */
    private String id;

    /**
     * App name
     */
    private String name;

    /**
     * App slug (URL-safe identifier)
     */
    private String slug;

    /**
     * App description
     */
    private String description;

    /**
     * Owner's user ID
     */
    private String ownerId;

    /**
     * Webhook URL for event notifications
     */
    private String webhookUrl;

    /**
     * CORS whitelist
     */
    private List<String> allowedOrigins;

    /**
     * Custom metadata
     */
    private Map<String, Object> customMetadata;

    /**
     * When app was created
     */
    private LocalDateTime createdAt;

    /**
     * When app was last updated
     */
    private LocalDateTime updatedAt;
}
