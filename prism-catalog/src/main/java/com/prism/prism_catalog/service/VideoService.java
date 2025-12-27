package com.prism.prism_catalog.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prism.prism_catalog.dto.CreateVideoRequest;
import com.prism.prism_catalog.dto.PagedResponse;
import com.prism.prism_catalog.dto.PublicVideoResponse;
import com.prism.prism_catalog.dto.UpdateAfterUploadRequest;
import com.prism.prism_catalog.dto.UpdateStreamsRequest;
import com.prism.prism_catalog.dto.UpdateVideoRequest;
import com.prism.prism_catalog.dto.VideoResponse;
import com.prism.prism_catalog.exception.ResourceNotFoundException;
import com.prism.prism_catalog.model.Video;
import com.prism.prism_catalog.model.enums.VideoStatus;
import com.prism.prism_catalog.model.enums.VideoVisibility;
import com.prism.prism_catalog.repository.VideoRepository;
import com.prism.prism_catalog.util.VideoMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;

    /**
     * Create a new video entry
     */
    @Transactional
    public VideoResponse createVideo(CreateVideoRequest request, String appId, String ownerUserName) {
        log.info("Creating video for appId: {}, owner: {}, title: {}", appId, ownerUserName, request.getTitle());

        Video video = videoMapper.toEntity(request, appId, ownerUserName);

        // Check for duplicate slug within the same app
        // if (videoRepository.findBySlugAndAppId(video.getSlug(), appId).isPresent()) {
        //     throw new IllegalArgumentException(
        //             "A video with slug '" + video.getSlug() + "' already exists for this app");
        // }

        Video saved = videoRepository.save(video);

        log.info("Created video with id: {}", saved.getId());
        return videoMapper.toResponse(saved);
    }

    /**
     * List videos for an app with filters and pagination
     */
    public PagedResponse<VideoResponse> listVideosForApp(
            String appId,
            VideoStatus status,
            String category,
            VideoVisibility visibility,
            String tag,
            String search,
            Instant fromDate,
            Instant toDate,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<Video> videoPage;

        // Apply filters based on parameters
        if (search != null && !search.isBlank()) {
            videoPage = videoRepository.searchByAppIdAndTitle(appId, search, pageable);
        } else if (tag != null && !tag.isBlank()) {
            videoPage = videoRepository.findByAppIdAndTag(appId, tag, pageable);
        } else if (fromDate != null && toDate != null) {
            videoPage = videoRepository.findByAppIdAndCreatedAtBetweenAndStatusNot(appId, fromDate, toDate,
                    VideoStatus.DELETED, pageable);
        } else if (status != null) {
            videoPage = videoRepository.findByAppIdAndStatus(appId, status, pageable);
        } else if (category != null && !category.isBlank()) {
            videoPage = videoRepository.findByAppIdAndCategoryAndStatusNot(appId, category, VideoStatus.DELETED,
                    pageable);
        } else if (visibility != null) {
            videoPage = videoRepository.findByAppIdAndVisibilityAndStatusNot(appId, visibility, VideoStatus.DELETED,
                    pageable);
        } else {
            videoPage = videoRepository.findByAppIdAndStatusNot(appId, VideoStatus.DELETED, pageable);
        }

        Page<VideoResponse> responsePage = videoPage.map(videoMapper::toResponse);
        return videoMapper.toPagedResponse(responsePage);
    }

    /**
     * Get a single video by ID for app owner
     */
    public VideoResponse getVideoForApp(String appId, String videoId) {
        Video video = videoRepository.findByIdAndAppId(videoId, appId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found or access denied"));

        if (video.getStatus() == VideoStatus.DELETED) {
            throw new ResourceNotFoundException("Video not found");
        }

        return videoMapper.toResponse(video);
    }

    /**
     * Get a single video by slug for app owner
     */
    public VideoResponse getVideoBySlugForApp(String appId, String slug) {
        Video video = videoRepository.findBySlugAndAppId(slug, appId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found or access denied"));

        if (video.getStatus() == VideoStatus.DELETED) {
            throw new ResourceNotFoundException("Video not found");
        }

        return videoMapper.toResponse(video);
    }

    /**
     * Update video metadata
     */
    @Transactional
    public VideoResponse updateVideoForApp(String appId, String videoId, UpdateVideoRequest request) {
        Video video = videoRepository.findByIdAndAppId(videoId, appId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found or access denied"));

        if (video.getStatus() == VideoStatus.DELETED) {
            throw new ResourceNotFoundException("Video not found");
        }

        // Apply updates (only non-null fields)
        if (request.getTitle() != null) {
            video.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            video.setDescription(request.getDescription());
        }
        if (request.getVisibility() != null) {
            video.setVisibility(request.getVisibility());
        }
        if (request.getCategory() != null) {
            video.setCategory(request.getCategory());
        }
        if (request.getTags() != null) {
            video.setTags(request.getTags());
        }

        video.setUpdatedAt(Instant.now());
        Video updated = videoRepository.save(video);

        log.info("Updated video: {}", videoId);
        return videoMapper.toResponse(updated);
    }

    /**
     * Soft delete a video
     */
    @Transactional
    public void deleteVideoForApp(String appId, String videoId) {
        Video video = videoRepository.findByIdAndAppId(videoId, appId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found or access denied"));

        if (video.getStatus() == VideoStatus.DELETED) {
            throw new ResourceNotFoundException("Video already deleted");
        }

        video.setStatus(VideoStatus.DELETED);
        video.setDeletedAt(Instant.now());
        video.setUpdatedAt(Instant.now());
        videoRepository.save(video);

        log.info("Soft deleted video: {}", videoId);
    }

    /**
     * List public videos with filters and pagination
     */
    public PagedResponse<PublicVideoResponse> listPublicVideos(
            String category,
            String tag,
            String search,
            Instant fromDate,
            Instant toDate,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<Video> videoPage;

        // Apply filters
        if (search != null && !search.isBlank()) {
            videoPage = videoRepository.searchPublicByTitle(search, pageable);
        } else if (tag != null && !tag.isBlank()) {
            videoPage = videoRepository.findPublicByTag(tag, pageable);
        } else if (fromDate != null && toDate != null) {
            videoPage = videoRepository.findByVisibilityAndStatusAndCreatedAtBetween(
                    VideoVisibility.PUBLIC, VideoStatus.READY, fromDate, toDate, pageable);
        } else if (category != null && !category.isBlank()) {
            videoPage = videoRepository.findByVisibilityAndStatusAndCategory(
                    VideoVisibility.PUBLIC, VideoStatus.READY, category, pageable);
        } else {
            videoPage = videoRepository.findByVisibilityAndStatus(
                    VideoVisibility.PUBLIC, VideoStatus.READY, pageable);
        }

        Page<PublicVideoResponse> responsePage = videoPage.map(videoMapper::toPublicResponse);
        return videoMapper.toPagedResponse(responsePage);
    }

    /**
     * Get a single public video by ID
     */
    public PublicVideoResponse getPublicVideo(String videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        if (video.getVisibility() != VideoVisibility.PUBLIC || video.getStatus() != VideoStatus.READY) {
            throw new ResourceNotFoundException("Video not found or not public");
        }

        return videoMapper.toPublicResponse(video);
    }

    /**
     * Update video after file upload (internal endpoint)
     */
    @Transactional
    public void updateAfterUpload(String videoId, UpdateAfterUploadRequest request) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        if (request.getStorageBasePath() != null) {
            video.setStorageBasePath(request.getStorageBasePath());
        }
        if (request.getStatus() != null) {
            video.setStatus(request.getStatus());
        }

        video.setUpdatedAt(Instant.now());
        videoRepository.save(video);

        log.info("Updated video {} after upload", videoId);
    }

    /**
     * Update video streams after transcoding (internal endpoint)
     */
    @Transactional
    public void updateStreams(String videoId, UpdateStreamsRequest request) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        video.setStatus(request.getStatus());

        if (request.getStatus() == VideoStatus.READY) {
            if (request.getHlsMasterUrl() != null) {
                video.setHlsMasterUrl(request.getHlsMasterUrl());
            }
            if (request.getHlsVariants() != null) {
                List<Video.HlsVariant> variants = request.getHlsVariants().stream()
                        .map(videoMapper::toHlsVariant)
                        .collect(Collectors.toList());
                video.setHlsVariants(variants);
            }
            if (request.getThumbnails() != null) {
                video.setThumbnails(videoMapper.toThumbnails(request.getThumbnails()));
            }
            if (request.getDurationSeconds() != null) {
                video.setDurationSeconds(request.getDurationSeconds());
            }
        } else if (request.getStatus() == VideoStatus.FAILED) {
            log.warn("Video {} processing failed: {}", videoId, request.getErrorMessage());
        }

        video.setUpdatedAt(Instant.now());
        videoRepository.save(video);

        log.info("Updated video {} streams with status: {}", videoId, request.getStatus());
    }

    /**
     * Create pageable with sorting
     */
    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy != null && !sortBy.isBlank() ? sortBy : "createdAt");
        return PageRequest.of(page, size, sort);
    }
}
