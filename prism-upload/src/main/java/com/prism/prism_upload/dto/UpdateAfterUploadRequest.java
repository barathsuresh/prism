package com.prism.prism_upload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAfterUploadRequest {
    private String storageBasePath;
    private String sourceFileName;
    private Long sourceFileSizeBytes;
    private String sourceContentType;
    private String status;
}
