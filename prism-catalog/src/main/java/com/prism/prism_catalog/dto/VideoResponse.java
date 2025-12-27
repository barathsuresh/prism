package com.prism.prism_catalog.dto;

import java.time.Instant;
import java.util.List;

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
public class VideoResponse {

    private String id;
    private String appId;
    private String ownerUserName;
    private String title;
    private String slug;
    private String description;
    private String category;
    private List<String> tags;
    private VideoStatus status;
    private VideoVisibility visibility;
    private Integer durationSeconds;
    private String hlsMasterUrl;
    private List<String> availableQualities;
    private String thumbnailSmallUrl;
    private String thumbnailMediumUrl;
    private String thumbnailLargeUrl;
    private Long viewsCount;
    private Long likesCount;
    private Instant createdAt;
    private Instant updatedAt;
}
