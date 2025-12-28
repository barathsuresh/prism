package com.prism.prism_upload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message sent from Upload Service to Transcoder Service via RabbitMQ
 * 
 * When a video is uploaded, this message is sent to RabbitMQ queue.
 * Transcoder Service listens to the queue and processes this message.
 * 
 * Message Flow:
 * 1. User uploads video
 * 2. Upload Service saves file to MinIO
 * 3. Upload Service creates TranscodeMessage with video details
 * 4. Upload Service sends message to RabbitMQ (video.exchange ->
 * video.transcode.queue)
 * 5. Transcoder Service receives message from queue
 * 6. Transcoder downloads file from MinIO using sourceFilePath
 * 7. Transcoder converts video to HLS format
 * 8. Transcoder uploads results back to MinIO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscodeMessage {
    /** Unique video identifier - used to update catalog after transcoding */
    private String videoId;

    /** Application identifier - for multi-tenancy */
    private String appId;

    /** Path to source video in MinIO - e.g., "app123/video456/source/movie.mp4" */
    private String sourceFilePath;

    /** Base path in MinIO - e.g., "app123/video456" - where to save HLS output */
    private String storageBasePath;

    /** Original filename - e.g., "movie.mp4" */
    private String fileName;

    /** File size in bytes - helps with progress tracking during transcoding */
    private Long fileSizeBytes;

    /** MIME type - e.g., "video/mp4" */
    private String contentType;
}
