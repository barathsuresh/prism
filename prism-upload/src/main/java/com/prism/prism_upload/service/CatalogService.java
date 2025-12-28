package com.prism.prism_upload.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.prism.prism_upload.dto.UpdateAfterUploadRequest;
import com.prism.prism_upload.dto.VideoInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final @Qualifier("catalogWebClient") WebClient catalogWebClient;

    /**
     * Fetch video info for an app to check current status
     */
    public Mono<VideoInfo> getVideo(String appId, String videoId) {
        return catalogWebClient.get()
                .uri("/api/catalog/videos/{videoId}", videoId)
                .header("X-App-Id", appId)
                .retrieve()
                .bodyToMono(VideoInfo.class)
                .doOnError(e -> {
                    if (e instanceof WebClientResponseException wcre) {
                        log.warn("Catalog GET video failed: status={}, videoId={}, appId={}", wcre.getStatusCode().value(),
                                videoId, appId);
                    } else {
                        log.warn("Catalog GET video failed: videoId={}, appId={}", videoId, appId, e);
                    }
                });
    }

    /**
     * Update catalog service after upload
     */
    public Mono<Void> updateAfterUpload(String videoId, UpdateAfterUploadRequest request) {
        log.info("Updating catalog for videoId: {} with status: {}", videoId, request.getStatus());

        return catalogWebClient.put()
                .uri("/api/catalog/internal/videos/{videoId}/upload", videoId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Successfully updated catalog for videoId: {}", videoId))
                .doOnError(error -> log.error("Failed to update catalog for videoId: {}", videoId, error));
    }
}
