package com.prism.prism_catalog.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.prism.prism_catalog.model.enums.VideoStatus;
import com.prism.prism_catalog.model.enums.VideoVisibility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "videos")
@CompoundIndex(name = "app_slug_unique", def = "{'appId': 1, 'slug': 1}", unique = true) // Ensure unique slug per app
public class Video {

    @Id
    private String id; // vid_<slug>_<yyyyMMddHHmmss>

    private String appId; // owner app (from X-App-Id)
    private String ownerUserName; // owner user ID

    // Descriptive metadata
    private String title;
    private String slug;
    private String description;
    private String category;
    private List<String> tags;

    // Lifecycle & visibility
    private VideoStatus status;
    private VideoVisibility visibility;

    // Streaming / storage
    private String storageBasePath; // e.g. prism/<appId>/<videoId>/
    private String hlsMasterUrl; // master .m3u8 URL or path
    private List<HlsVariant> hlsVariants;

    // Thumbnails
    private Thumbnails thumbnails;

    // Metrics / extra
    private Integer durationSeconds;
    private Long viewsCount;
    private Long likesCount;

    // Audit / soft delete
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HlsVariant {
        private String quality; // "360p", "480p", "720p", "1080p"
        private Integer bitrateKbps; // optional
        private String url; // URL/path to variant playlist
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Thumbnails {
        private String smallUrl;
        private String mediumUrl;
        private String largeUrl;
    }
}