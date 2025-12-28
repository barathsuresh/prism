package com.prism.prism_transcoder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HlsVariantDto {
    private String quality; // e.g., 360p, 480p, 720p, 1080p
    private Integer bitrateKbps;
    private String url; // full URL to variant playlist
}
