package com.prism.prism_catalog.dto;

import com.prism.prism_catalog.model.enums.VideoStatus;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAfterUploadRequest {

    @Size(max = 500, message = "Storage base path must not exceed 500 characters")
    private String storageBasePath;

    @Size(max = 255, message = "Source file name must not exceed 255 characters")
    private String sourceFileName;

    private Long sourceFileSizeBytes;

    @Size(max = 100, message = "Content type must not exceed 100 characters")
    private String sourceContentType;

    private VideoStatus status; // UPLOADED or PROCESSING
}
