package com.prism.prism_transcoder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscodeMessage {
    private String videoId;
    private String appId;
    private String sourceFilePath; // e.g., app123/video456/source/movie.mp4
    private String storageBasePath; // e.g., app123/video456
    private String fileName;
    private Long fileSizeBytes;
    private String contentType;
}
