package com.prism.prism_stream.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.prism.prism_stream.dto.VideoPublicLite;
import com.prism.prism_stream.dto.VideoResponseLite;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final @Qualifier("catalogWebClient") WebClient catalogWebClient;

    /**
     * Fetch full video info for an app; requires X-App-Id header
     */
    public Mono<VideoResponseLite> getVideo(String appId, String videoId) {
        log.debug("[STREAM-SERVICE] Fetching video from catalog - videoId: {}, appId: {}", videoId, appId);
        return catalogWebClient.get()
                .uri("/api/catalog/videos/{videoId}", videoId)
                .header("X-App-Id", appId)
                .retrieve()
                .bodyToMono(VideoResponseLite.class)
                .doOnError(e -> log.warn("[STREAM-SERVICE] Failed to fetch video - videoId: {}, error: {}", videoId,
                        e.getMessage()));
    }

    /**
     * Fetch public video info (no app header required)
     */
    public Mono<VideoPublicLite> getPublicVideo(String videoId) {
        log.debug("[STREAM-SERVICE] Fetching public video from catalog - videoId: {}", videoId);
        return catalogWebClient.get()
                .uri("/api/catalog/videos/public/{videoId}", videoId)
                .retrieve()
                .bodyToMono(VideoPublicLite.class)
                .doOnError(e -> log.warn("[STREAM-SERVICE] Failed to fetch public video - videoId: {}, error: {}",
                        videoId, e.getMessage()));
    }
}
