package com.prism.prism_transcoder.dto;

import java.util.List;

import com.prism.prism_transcoder.model.VideoStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStreamsRequest {
    private VideoStatus status; // READY, PROCESSING, FAILED
    private Integer durationSeconds; // optional
    private String hlsMasterUrl; // URL to master.m3u8
    private List<HlsVariantDto> hlsVariants; // quality variants
    private ThumbnailsDto thumbnails; // optional
    private String errorMessage; // only for FAILED
}
