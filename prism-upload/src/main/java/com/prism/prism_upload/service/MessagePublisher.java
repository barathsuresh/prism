package com.prism.prism_upload.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.prism.prism_upload.config.RabbitMQConfig;
import com.prism.prism_upload.dto.TranscodeMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Message Publisher - Sends transcode jobs to RabbitMQ
 * 
 * This service publishes messages to RabbitMQ after a video is uploaded.
 * Think of it as dropping a letter in a mailbox - you don't wait for delivery,
 * you just drop it and continue with other work.
 * 
 * Flow:
 * 1. Video uploaded to MinIO
 * 2. Call publishTranscodeMessage() with video details
 * 3. Message is sent to RabbitMQ (non-blocking, instant)
 * 4. Upload Service continues processing other requests
 * 5. Transcoder Service picks up message when ready
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessagePublisher {

    // RabbitTemplate - Spring's tool for sending messages to RabbitMQ
    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish transcode message to RabbitMQ
     * 
     * convertAndSend() does 3 things:
     * 1. Converts Java object (TranscodeMessage) to JSON using
     * JacksonJsonMessageConverter
     * 2. Sends message to specified exchange with routing key
     * 3. RabbitMQ routes message to queue based on binding rules
     * 
     * Parameters:
     * - Exchange: "video.exchange" - where message is sent
     * - Routing Key: "video.transcode" - label for routing
     * - Message: TranscodeMessage object (auto-converted to JSON)
     * 
     * Result:
     * Message arrives in "video.transcode.queue" and waits for transcoder to
     * process it
     */
    public Mono<Void> publishTranscodeMessage(TranscodeMessage message) {
        return Mono.fromRunnable(() -> {
            log.info("Publishing transcode message for videoId: {}", message.getVideoId());

            // Send message to RabbitMQ
            // This is asynchronous - doesn't wait for transcoder to process it
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.VIDEO_EXCHANGE, // Where to send (exchange)
                    RabbitMQConfig.TRANSCODE_ROUTING_KEY, // How to route (routing key)
                    message); // What to send (payload)

            log.info("Successfully published transcode message for videoId: {}", message.getVideoId());
        });
    }
}
