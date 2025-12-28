package com.prism.prism_transcoder.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.prism.prism_transcoder.dto.UpdateStreamsRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogClient {

    private final WebClient.Builder webClientBuilder;

    public Mono<Void> updateStreams(String videoId, UpdateStreamsRequest request) {
        WebClient client = webClientBuilder.build();

        // Using service discovery name 'prism-catalog'
        String url = String.format("http://prism-catalog/api/catalog/internal/videos/%s/streams", videoId);
        log.info("Updating catalog streams: {}", url);
        return client.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Catalog updated for videoId={}", videoId))
                .doOnError(e -> log.error("Failed to update catalog for videoId={}", videoId, e));
    }
}
