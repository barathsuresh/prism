package com.prism.prism_catalog.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThumbnailsDto {

    @Size(max = 1000, message = "Small URL must not exceed 1000 characters")
    private String smallUrl;

    @Size(max = 1000, message = "Medium URL must not exceed 1000 characters")
    private String mediumUrl;

    @Size(max = 1000, message = "Large URL must not exceed 1000 characters")
    private String largeUrl;
}
