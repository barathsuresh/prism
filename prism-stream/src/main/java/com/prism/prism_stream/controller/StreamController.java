package com.prism.prism_stream.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prism.prism_stream.service.StreamService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class StreamController {

    private final StreamService streamService;

    /**
     * Redirect to HLS master playlist URL stored in Catalog
     */
    @GetMapping("/videos/{videoId}/master")
    public Mono<ResponseEntity<Void>> getMaster(@RequestHeader("X-App-Id") String appId,
            @PathVariable String videoId) {
        return streamService.getMasterPlaylistUrl(appId, videoId)
                .map(url -> ResponseEntity.status(302)
                        .header(HttpHeaders.LOCATION, url)
                        .build());
    }

    /**
     * Redirect to thumbnail URL (size: small|medium|large)
     */
    @GetMapping("/videos/{videoId}/thumbnail")
    public Mono<ResponseEntity<Void>> getThumbnail(@RequestHeader("X-App-Id") String appId,
            @PathVariable String videoId,
            @RequestParam(defaultValue = "medium") String size) {
        return streamService.getThumbnailUrl(appId, videoId, size)
                .map(url -> ResponseEntity.status(302)
                        .header(HttpHeaders.LOCATION, url)
                        .build());
    }

    /**
     * PUBLIC: Redirect to HLS master playlist URL (no app header)
     */
    @GetMapping("/public/videos/{videoId}/master")
    public Mono<ResponseEntity<Void>> getPublicMaster(@PathVariable String videoId) {
        return streamService.getPublicMasterPlaylistUrl(videoId)
                .map(url -> ResponseEntity.status(302)
                        .header(HttpHeaders.LOCATION, url)
                        .build());
    }

    /**
     * PUBLIC: Redirect to thumbnail URL (size: small|medium|large)
     */
    @GetMapping("/public/videos/{videoId}/thumbnail")
    public Mono<ResponseEntity<Void>> getPublicThumbnail(@PathVariable String videoId,
            @RequestParam(defaultValue = "medium") String size) {
        return streamService.getPublicThumbnailUrl(videoId, size)
                .map(url -> ResponseEntity.status(302)
                        .header(HttpHeaders.LOCATION, url)
                        .build());
    }
}
