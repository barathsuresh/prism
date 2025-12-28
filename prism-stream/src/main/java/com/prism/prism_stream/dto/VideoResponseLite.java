package com.prism.prism_stream.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponseLite {
    private String id;
    private String appId;
    private String hlsMasterUrl;
    private String thumbnailSmallUrl;
    private String thumbnailMediumUrl;
    private String thumbnailLargeUrl;
}
