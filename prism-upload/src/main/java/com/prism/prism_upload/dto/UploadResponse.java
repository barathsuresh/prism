package com.prism.prism_upload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    private String videoId;
    private String message;
    private String fileName;
    private Long fileSize;
    private String storageBasePath;
    private String status;
}
