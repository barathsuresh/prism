package com.prism.prism_transcoder.listener;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prism.prism_transcoder.dto.TranscodeMessage;
import com.prism.prism_transcoder.dto.UpdateStreamsRequest;
import com.prism.prism_transcoder.model.VideoStatus;
import com.prism.prism_transcoder.service.CatalogClient;
import com.prism.prism_transcoder.service.TranscodeJobService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TranscodeListener {

    private final TranscodeJobService jobService;
    private final CatalogClient catalogClient;
    private final ObjectMapper objectMapper;

    // TODO(concurrency): Consider configuring listener concurrency
    // (consumers/prefetch)
    // via the container factory or properties (e.g., setConcurrentConsumers,
    // setPrefetchCount). Keep single-consumer per instance until we benchmark
    // FFmpeg
    // throughput and CPU usage.
    @RabbitListener(queues = "video.transcode.queue")
    public void onMessage(@Payload byte[] payload) {
        TranscodeMessage msg;
        try {
            msg = objectMapper.readValue(payload, TranscodeMessage.class);
        } catch (Exception e) {
            log.error("Failed to deserialize TranscodeMessage payload", e);
            throw new RuntimeException("Bad message payload", e);
        }
        log.info("Received transcode message: videoId={}, appId={}", msg.getVideoId(), msg.getAppId());
        try {
            jobService.process(msg);
            log.info("Transcode job finished: videoId={}", msg.getVideoId());
        } catch (Exception e) {
            log.error("Transcode job failed: videoId={}", msg.getVideoId(), e);
            try {
                UpdateStreamsRequest failed = UpdateStreamsRequest.builder()
                        .status(VideoStatus.FAILED)
                        .errorMessage(e.getMessage())
                        .build();
                catalogClient.updateStreams(msg.getVideoId(), failed).block();
            } catch (Exception ignore) {
                log.warn("Failed to update catalog with FAILED status for videoId={}", msg.getVideoId());
            }
            // Reject and DO NOT requeue to avoid infinite retry loops
            throw new AmqpRejectAndDontRequeueException("Transcode failed", e);
        }
    }
}
