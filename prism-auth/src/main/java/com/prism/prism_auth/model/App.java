package com.prism.prism_auth.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * App entity representing a developer's application.
 *
 * A user can create multiple apps (e.g., "Fitness App Prod", "Fitness App
 * Beta").
 * Each app has its own set of API keys and video data.
 */
@Document(collection = "apps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class App {

    /**
     * MongoDB ObjectId (auto-generated)
     */
    @Id
    private String id;

    /**
     * Human-readable app name (e.g., "Fitness App Production")
     */
    private String name;

    /**
     * URL-safe slug for the app (e.g., "fitness-app-production")
     * UNIQUE index with ownerId for tenant isolation
     */
    @Indexed(unique = true)
    private String slug;

    /**
     * Optional description
     */
    private String description;

    /**
     * The User ID who owns this app
     * Indexed for quick lookups of "all apps for a user"
     */
    @Indexed
    private String ownerId;

    /**
     * Optional: Organization ID (for multi-tenant support in future)
     */
    private String organizationId;

    /**
     * CORS whitelist - which domains can make requests to Prism using keys from
     * this app
     * Example: ["https://app.example.com", "https://api.example.com"]
     */
    private List<String> allowedOrigins;

    /**
     * Webhook URL for event notifications
     * Prism can send events like "video_transcoded" to this URL
     */
    private String webhookUrl;

    /**
     * Flexible metadata for custom configuration
     * Examples: {"max_video_duration": 3600, "custom_prefix": "myapp"}
     */
    private Map<String, Object> customMetadata;

    /**
     * Audit fields - automatically managed
     */
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Soft delete - set when app is deleted
     */
    private LocalDateTime deletedAt;

    /**
     * User ID who created this app (usually same as ownerId)
     */
    private String createdBy;

    /**
     * Helper method to check if app is active (not deleted)
     */
    public boolean isActive() {
        return deletedAt == null;
    }

    /**
     * Helper method for quick CORS validation
     */
    public boolean isOriginAllowed(String origin) {
        return allowedOrigins != null && allowedOrigins.contains(origin);
    }

    /**
     * Helper method to check if app is soft deleted
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
