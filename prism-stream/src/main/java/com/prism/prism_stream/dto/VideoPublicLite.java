package com.prism.prism_stream.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoPublicLite {
    private String id;
    private String hlsMasterUrl;
    private String thumbnailSmallUrl;
    private String thumbnailMediumUrl;
    private String thumbnailLargeUrl;
}
