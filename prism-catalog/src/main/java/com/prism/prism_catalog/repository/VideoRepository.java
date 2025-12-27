package com.prism.prism_catalog.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.prism.prism_catalog.model.Video;
import com.prism.prism_catalog.model.enums.VideoStatus;
import com.prism.prism_catalog.model.enums.VideoVisibility;

@Repository
public interface VideoRepository extends MongoRepository<Video, String> {

    /**
     * Find by ID and appId (ownership verification)
     */
    Optional<Video> findByIdAndAppId(String id, String appId);

    /**
     * Find by slug and appId (ownership verification)
     */
    Optional<Video> findBySlugAndAppId(String slug, String appId);

    /**
     * List all videos for an app (excluding deleted)
     */
    Page<Video> findByAppIdAndStatusNot(String appId, VideoStatus status, Pageable pageable);

    /**
     * List videos for an app with status filter
     */
    Page<Video> findByAppIdAndStatus(String appId, VideoStatus status, Pageable pageable);

    /**
     * List videos for an app with category filter
     */
    Page<Video> findByAppIdAndCategoryAndStatusNot(String appId, String category, VideoStatus status,
            Pageable pageable);

    /**
     * List videos for an app with visibility filter
     */
    Page<Video> findByAppIdAndVisibilityAndStatusNot(String appId, VideoVisibility visibility, VideoStatus status,
            Pageable pageable);

    /**
     * List public videos (PUBLIC visibility + READY status)
     */
    Page<Video> findByVisibilityAndStatus(VideoVisibility visibility, VideoStatus status, Pageable pageable);

    /**
     * List public videos with category filter
     */
    Page<Video> findByVisibilityAndStatusAndCategory(VideoVisibility visibility, VideoStatus status, String category,
            Pageable pageable);

    /**
     * Search videos by title (for app owner)
     */
    @Query("{ 'appId': ?0, 'title': { $regex: ?1, $options: 'i' }, 'status': { $ne: 'DELETED' } }")
    Page<Video> searchByAppIdAndTitle(String appId, String titlePattern, Pageable pageable);

    /**
     * Search public videos by title
     */
    @Query("{ 'visibility': 'PUBLIC', 'status': 'READY', 'title': { $regex: ?0, $options: 'i' } }")
    Page<Video> searchPublicByTitle(String titlePattern, Pageable pageable);

    /**
     * Find videos by tag (for app owner)
     */
    @Query("{ 'appId': ?0, 'tags': ?1, 'status': { $ne: 'DELETED' } }")
    Page<Video> findByAppIdAndTag(String appId, String tag, Pageable pageable);

    /**
     * Find public videos by tag
     */
    @Query("{ 'visibility': 'PUBLIC', 'status': 'READY', 'tags': ?0 }")
    Page<Video> findPublicByTag(String tag, Pageable pageable);

    /**
     * Find videos created in date range (for app owner)
     */
    Page<Video> findByAppIdAndCreatedAtBetweenAndStatusNot(String appId, Instant from, Instant to, VideoStatus status,
            Pageable pageable);

    /**
     * Find public videos created in date range
     */
    Page<Video> findByVisibilityAndStatusAndCreatedAtBetween(VideoVisibility visibility, VideoStatus status,
            Instant from, Instant to, Pageable pageable);
}
