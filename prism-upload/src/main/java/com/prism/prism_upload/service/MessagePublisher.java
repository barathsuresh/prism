package com.prism.prism_upload.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.prism.prism_upload.config.RabbitMQConfig;
import com.prism.prism_upload.dto.TranscodeMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish transcode message to RabbitMQ
     */
    public Mono<Void> publishTranscodeMessage(TranscodeMessage message) {
        return Mono.fromRunnable(() -> {
            log.info("Publishing transcode message for videoId: {}", message.getVideoId());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.VIDEO_EXCHANGE,
                    RabbitMQConfig.TRANSCODE_ROUTING_KEY,
                    message);
            log.info("Successfully published transcode message for videoId: {}", message.getVideoId());
        });
    }
}
