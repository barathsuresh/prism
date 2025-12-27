package com.prism.prism_upload.dto;

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
    private String sourceFilePath;
    private String storageBasePath;
    private String fileName;
    private Long fileSizeBytes;
    private String contentType;
}
