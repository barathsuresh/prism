package com.prism.prism_transcoder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThumbnailsDto {
    private String smallUrl;
    private String mediumUrl;
    private String largeUrl;
}
