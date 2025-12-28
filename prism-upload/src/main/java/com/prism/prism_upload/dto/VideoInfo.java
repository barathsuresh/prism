package com.prism.prism_upload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoInfo {
    private String id;
    private String appId;
    private String status; // Use string to avoid tight coupling; map as needed
}
