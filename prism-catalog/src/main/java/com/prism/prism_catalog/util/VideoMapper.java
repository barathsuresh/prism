package com.prism.prism_catalog.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.prism.prism_catalog.dto.CreateVideoRequest;
import com.prism.prism_catalog.dto.HlsVariantDto;
import com.prism.prism_catalog.dto.PagedResponse;
import com.prism.prism_catalog.dto.PublicVideoResponse;
import com.prism.prism_catalog.dto.ThumbnailsDto;
import com.prism.prism_catalog.dto.VideoResponse;
import com.prism.prism_catalog.model.Video;
import com.prism.prism_catalog.model.enums.VideoStatus;
import com.prism.prism_catalog.model.enums.VideoVisibility;

@Component
public class VideoMapper {

    private static final DateTimeFormatter VIDEO_ID_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Map CreateVideoRequest to Video entity
     */
    public Video toEntity(CreateVideoRequest request, String appId, String ownerUserName) {
        Instant now = Instant.now();
        String title = request.getTitle();
        String slug = generateSlug(title);
        String videoId = generateVideoId(title, now);

        return Video.builder()
                .id(videoId)
                .appId(appId)
                .ownerUserName(ownerUserName)
                .title(title)
                .slug(slug)
                .description(request.getDescription())
                .category(request.getCategory())
                .tags(request.getTags() != null ? request.getTags() : Collections.emptyList())
                .status(VideoStatus.PENDING)
                .visibility(request.getVisibility() != null ? request.getVisibility() : VideoVisibility.PRIVATE)
                .viewsCount(0L)
                .likesCount(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Map Video entity to VideoResponse DTO
     */
    public VideoResponse toResponse(Video video) {
        return VideoResponse.builder()
                .id(video.getId())
                .appId(video.getAppId())
                .ownerUserName(video.getOwnerUserName())
                .title(video.getTitle())
                .slug(video.getSlug())
                .description(video.getDescription())
                .category(video.getCategory())
                .tags(video.getTags())
                .status(video.getStatus())
                .visibility(video.getVisibility())
                .durationSeconds(video.getDurationSeconds())
                .hlsMasterUrl(video.getHlsMasterUrl())
                .availableQualities(extractQualities(video.getHlsVariants()))
                .thumbnailSmallUrl(video.getThumbnails() != null ? video.getThumbnails().getSmallUrl() : null)
                .thumbnailMediumUrl(video.getThumbnails() != null ? video.getThumbnails().getMediumUrl() : null)
                .thumbnailLargeUrl(video.getThumbnails() != null ? video.getThumbnails().getLargeUrl() : null)
                .viewsCount(video.getViewsCount())
                .likesCount(video.getLikesCount())
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .build();
    }

    /**
     * Map Video entity to PublicVideoResponse DTO
     */
    public PublicVideoResponse toPublicResponse(Video video) {
        return PublicVideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .slug(video.getSlug())
                .description(video.getDescription())
                .category(video.getCategory())
                .tags(video.getTags())
                .visibility(video.getVisibility())
                .durationSeconds(video.getDurationSeconds())
                .availableQualities(extractQualities(video.getHlsVariants()))
                .thumbnailSmallUrl(video.getThumbnails() != null ? video.getThumbnails().getSmallUrl() : null)
                .thumbnailMediumUrl(video.getThumbnails() != null ? video.getThumbnails().getMediumUrl() : null)
                .thumbnailLargeUrl(video.getThumbnails() != null ? video.getThumbnails().getLargeUrl() : null)
                .viewsCount(video.getViewsCount())
                .likesCount(video.getLikesCount())
                .createdAt(video.getCreatedAt())
                .build();
    }

    /**
     * Map HlsVariantDto to Video.HlsVariant
     */
    public Video.HlsVariant toHlsVariant(HlsVariantDto dto) {
        return Video.HlsVariant.builder()
                .quality(dto.getQuality())
                .bitrateKbps(dto.getBitrateKbps())
                .url(dto.getUrl())
                .build();
    }

    /**
     * Map ThumbnailsDto to Video.Thumbnails
     */
    public Video.Thumbnails toThumbnails(ThumbnailsDto dto) {
        if (dto == null) {
            return null;
        }
        return Video.Thumbnails.builder()
                .smallUrl(dto.getSmallUrl())
                .mediumUrl(dto.getMediumUrl())
                .largeUrl(dto.getLargeUrl())
                .build();
    }

    /**
     * Create paged response from Page
     */
    public <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .items(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    /**
     * Generate video ID from title and timestamp
     * Format: vid_<slugified-title>_<yyyyMMddHHmmss>
     */
    private String generateVideoId(String title, Instant createdAt) {
        String slug = generateSlug(title);
        LocalDateTime dateTime = LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault());
        String timestamp = dateTime.format(VIDEO_ID_DATE_FORMAT);
        return "vid_" + slug + "_" + timestamp;
    }

    /**
     * Generate URL-safe slug from title
     */
    private String generateSlug(String title) {
        if (title == null || title.isBlank()) {
            return "untitled";
        }
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special chars
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Remove consecutive hyphens
                .replaceAll("^-|-$", "") // Remove leading/trailing hyphens
                .substring(0, Math.min(50, title.length())); // Limit length
    }

    /**
     * Extract quality list from HLS variants
     */
    private List<String> extractQualities(List<Video.HlsVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return Collections.emptyList();
        }
        return variants.stream()
                .map(Video.HlsVariant::getQuality)
                .collect(Collectors.toList());
    }
}
