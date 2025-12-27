package com.prism.prism_catalog.dto;

import java.util.List;

import com.prism.prism_catalog.model.enums.VideoStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStreamsRequest {

    @NotNull(message = "Status is required")
    private VideoStatus status; // READY, PROCESSING, or FAILED

    private Integer durationSeconds;

    @Size(max = 1000, message = "HLS master URL must not exceed 1000 characters")
    private String hlsMasterUrl;

    private List<HlsVariantDto> hlsVariants;

    private ThumbnailsDto thumbnails;

    @Size(max = 1000, message = "Error message must not exceed 1000 characters")
    private String errorMessage; // for FAILED status
}
