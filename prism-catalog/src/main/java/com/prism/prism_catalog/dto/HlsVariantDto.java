package com.prism.prism_catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HlsVariantDto {

    @NotBlank(message = "Quality is required")
    @Size(max = 20, message = "Quality must not exceed 20 characters")
    private String quality; // e.g., "360p", "480p", "720p", "1080p"

    private Integer bitrateKbps;

    @NotBlank(message = "URL is required")
    @Size(max = 1000, message = "URL must not exceed 1000 characters")
    private String url;
}
