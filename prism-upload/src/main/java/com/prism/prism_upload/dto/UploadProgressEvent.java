package com.prism.prism_upload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadProgressEvent {
    private String videoId;
    private long bytesUploaded;
    private long totalBytes;
    private int percentage;
    private String status; // UPLOADING, COMPLETED, FAILED
    private String message;
}
