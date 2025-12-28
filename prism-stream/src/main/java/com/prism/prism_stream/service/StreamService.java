package com.prism.prism_stream.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class StreamService {

    private final CatalogService catalogService;
    private final MinIOPresignService presignService;

    public Mono<String> getMasterPlaylistUrl(String appId, String videoId) {
        return catalogService.getVideo(appId, videoId)
                .flatMap(v -> {
                    String hlsUrl = v.getHlsMasterUrl();
                    if (hlsUrl == null || hlsUrl.isBlank()) {
                        return Mono.error(new IllegalArgumentException(
                                "Video transcoding not yet complete or HLS URL is missing"));
                    }
                    String key = extractObjectKeyFromUrl(hlsUrl);
                    if (key == null) {
                        return Mono.error(new IllegalArgumentException("Unable to extract object key from HLS URL"));
                    }
                    return Mono.just(presignService.generatePresignedUrl(key));
                });
    }

    public Mono<String> getThumbnailUrl(String appId, String videoId, String size) {
        return catalogService.getVideo(appId, videoId)
                .flatMap(v -> {
                    String thumbUrl = switch (size) {
                        case "small" -> v.getThumbnailSmallUrl();
                        case "medium" -> v.getThumbnailMediumUrl();
                        case "large" -> v.getThumbnailLargeUrl();
                        default -> v.getThumbnailMediumUrl();
                    };
                    if (thumbUrl == null || thumbUrl.isBlank()) {
                        return Mono.error(
                                new IllegalArgumentException("Thumbnail URL for size " + size + " not available"));
                    }
                    String key = extractObjectKeyFromUrl(thumbUrl);
                    if (key == null) {
                        return Mono
                                .error(new IllegalArgumentException("Unable to extract object key from thumbnail URL"));
                    }
                    return Mono.just(presignService.generatePresignedUrl(key));
                });
    }

    public Mono<String> getPublicMasterPlaylistUrl(String videoId) {
        return catalogService.getPublicVideo(videoId)
                .flatMap(v -> {
                    String hlsUrl = v.getHlsMasterUrl();
                    if (hlsUrl == null || hlsUrl.isBlank()) {
                        return Mono.error(new IllegalArgumentException(
                                "Video transcoding not yet complete or HLS URL is missing"));
                    }
                    String key = extractObjectKeyFromUrl(hlsUrl);
                    if (key == null) {
                        return Mono.error(new IllegalArgumentException("Unable to extract object key from HLS URL"));
                    }
                    return Mono.just(presignService.generatePresignedUrl(key));
                });
    }

    public Mono<String> getPublicThumbnailUrl(String videoId, String size) {
        return catalogService.getPublicVideo(videoId)
                .flatMap(v -> {
                    String thumbUrl = switch (size) {
                        case "small" -> v.getThumbnailSmallUrl();
                        case "medium" -> v.getThumbnailMediumUrl();
                        case "large" -> v.getThumbnailLargeUrl();
                        default -> v.getThumbnailMediumUrl();
                    };
                    if (thumbUrl == null || thumbUrl.isBlank()) {
                        return Mono.error(
                                new IllegalArgumentException("Thumbnail URL for size " + size + " not available"));
                    }
                    String key = extractObjectKeyFromUrl(thumbUrl);
                    if (key == null) {
                        return Mono
                                .error(new IllegalArgumentException("Unable to extract object key from thumbnail URL"));
                    }
                    return Mono.just(presignService.generatePresignedUrl(key));
                });
    }

    /**
     * Extract S3 object key from MinIO URL
     * Handles both:
     * - s3://prism-videos/appId/vidId/hls/master.m3u8 (direct S3 path)
     * - http://minio:9000/prism-videos/appId/vidId/hls/master.m3u8 (MinIO URL)
     */
    private String extractObjectKeyFromUrl(String urlOrPath) {
        if (urlOrPath == null) {
            return null;
        }

        // Remove s3:// prefix if present
        if (urlOrPath.startsWith("s3://")) {
            String withoutScheme = urlOrPath.substring(5);
            int slashIdx = withoutScheme.indexOf('/');
            if (slashIdx > 0) {
                return withoutScheme.substring(slashIdx + 1);
            }
        }

        // Handle HTTP URLs: extract everything after bucket name
        if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
            // Format: http://minio:9000/prism-videos/appId/vidId/...
            int bucketStart = urlOrPath.indexOf("/prism-videos/");
            if (bucketStart > 0) {
                return urlOrPath.substring(bucketStart + 1 + "prism-videos".length()).substring(1);
            }
        }

        // Already an object key
        return urlOrPath;
    }
}
