package com.prism.prism_stream.controller;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prism.prism_stream.config.MinIOConfig;
import com.prism.prism_stream.service.CatalogService;
import com.prism.prism_stream.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/stream")
@RequiredArgsConstructor
@Slf4j
public class SmartProxyController {

    private final CatalogService catalogService;
    private final S3Service s3Service;
    private final MinIOConfig minioConfig;

    /**
     * GET /stream/{videoId}/master.m3u8
     * Validate access, fetch master.m3u8 from MinIO, rewrite variant paths to point
     * back to this service.
     */
    @GetMapping(value = "/{videoId}/master.m3u8", produces = "application/vnd.apple.mpegurl")
    public Mono<ResponseEntity<String>> proxyMaster(
            @RequestHeader("X-App-Id") String appId,
            @PathVariable String videoId) {

        log.info("Private master request: videoId={}, appId={}", videoId, appId);
        return catalogService.getVideo(appId, videoId)
                .flatMap(v -> {
                    String key = s3Service.extractObjectKeyFromUrl(v.getHlsMasterUrl(), minioConfig.getBucket());
                    if (key == null || key.isBlank()) {
                        return Mono.error(new IllegalArgumentException("HLS master URL is not available"));
                    }
                    return s3Service.getObjectAsString(minioConfig.getBucket(), key)
                            .map(content -> rewriteMasterContent(content, "/stream/" + videoId));
                })
                .map(rewritten -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                        .body(rewritten));
    }

    /**
     * Variant playlist proxy for private videos:
     * /stream/{videoId}/{quality}/playlist.m3u8
     */
    @GetMapping(value = "/{videoId}/{quality}/playlist.m3u8", produces = "application/vnd.apple.mpegurl")
    public Mono<ResponseEntity<String>> proxyVariantPlaylist(
            @RequestHeader("X-App-Id") String appId,
            @PathVariable String videoId,
            @PathVariable String quality) {

        log.info("Private variant playlist request: videoId={}, quality={}, appId={}", videoId, quality, appId);
        return catalogService.getVideo(appId, videoId)
                .flatMap(v -> {
                    String masterKey = s3Service.extractObjectKeyFromUrl(v.getHlsMasterUrl(), minioConfig.getBucket());
                    if (masterKey == null) {
                        return Mono.error(new IllegalArgumentException("HLS master URL is not available"));
                    }
                    String base = baseHlsPrefix(masterKey);
                    String variantKey = base + "/" + quality + "/playlist.m3u8";
                    return s3Service.getObjectAsString(minioConfig.getBucket(), variantKey)
                            .map(content -> rewriteVariantContent(content, "/stream/" + videoId, quality));
                })
                .map(rewritten -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                        .body(rewritten));
    }

    /**
     * GET /stream/{videoId}/{quality}/{segment}.ts
     * Validate access, stream TS segment from MinIO without loading into RAM.
     */
    @GetMapping(value = "/{videoId}/{quality}/{segment}.ts", produces = "video/mp2t")
    public Mono<ResponseEntity<Flux<ByteBuffer>>> proxySegment(
            @RequestHeader("X-App-Id") String appId,
            @PathVariable String videoId,
            @PathVariable String quality,
            @PathVariable String segment) {

        log.info("Private segment request: videoId={}, quality={}, segment={}, appId={}", videoId, quality, segment,
                appId);
        return catalogService.getVideo(appId, videoId)
                .flatMap(v -> {
                    String masterKey = s3Service.extractObjectKeyFromUrl(v.getHlsMasterUrl(), minioConfig.getBucket());
                    if (masterKey == null) {
                        return Mono.error(new IllegalArgumentException("HLS master URL is not available"));
                    }
                    String base = baseHlsPrefix(masterKey);
                    String segmentKey = base + "/" + quality + "/" + segment + ".ts";
                    Flux<ByteBuffer> stream = s3Service.streamObject(minioConfig.getBucket(), segmentKey);
                    return Mono.just(ResponseEntity.ok()
                            .contentType(MediaType.valueOf("video/mp2t"))
                            .body(stream));
                });
    }

    private String baseHlsPrefix(String masterKey) {
        int idx = masterKey.indexOf("/hls/");
        if (idx > 0) {
            return masterKey.substring(0, idx + 4); // include '/hls'
        }
        // Fallback: remove 'master.m3u8'
        if (masterKey.endsWith("master.m3u8")) {
            return masterKey.substring(0, masterKey.length() - "master.m3u8".length());
        }
        return masterKey;
    }

    /**
     * Rewrite variant playlist paths inside master.m3u8 to call back into this
     * service.
     * Examples:
     * - "720p/playlist.m3u8" => "/stream/{videoId}/720p/playlist.m3u8"
     * - "http://minio:9000/bucket/.../hls/720p/playlist.m3u8" =>
     * "/stream/{videoId}/720p/playlist.m3u8"
     */
    private String rewriteMasterContent(String content, String basePath) {
        // Match lines that end with '/playlist.m3u8' and capture the quality folder
        Pattern rel = Pattern.compile("(?:^|.*/)([0-9]{3,4}p)/playlist\\.m3u8", Pattern.CASE_INSENSITIVE);
        String[] lines = content.split("\r?\n");
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith("#")) { // keep directives
                out.append(line).append('\n');
                continue;
            }
            Matcher m = rel.matcher(line);
            if (m.find()) {
                String q = m.group(1);
                out.append(basePath).append("/").append(q).append("/playlist.m3u8").append('\n');
            } else {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }

    /**
     * Rewrite segment lines inside variant playlist to call back into this service.
     * e.g. "segment000.ts" => "/stream/{videoId}/{quality}/segment000.ts"
     */
    private String rewriteVariantContent(String content, String basePath, String quality) {
        String[] lines = content.split("\r?\n");
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith("#")) {
                out.append(line).append('\n');
                continue;
            }
            if (line.endsWith(".ts")) {
                out.append(basePath).append("/").append(quality).append("/").append(line).append('\n');
            } else {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }

    /**
     * PUBLIC: master manifest proxy
     */
    @GetMapping(value = "/public/{videoId}/master.m3u8", produces = "application/vnd.apple.mpegurl")
    public Mono<ResponseEntity<String>> proxyPublicMaster(@PathVariable String videoId) {
        log.info("Public master request: videoId={}", videoId);
        return catalogService.getPublicVideo(videoId)
                .flatMap(v -> {
                    String key = s3Service.extractObjectKeyFromUrl(v.getHlsMasterUrl(), minioConfig.getBucket());
                    if (key == null || key.isBlank()) {
                        return Mono.error(new IllegalArgumentException("HLS master URL is not available"));
                    }
                    return s3Service.getObjectAsString(minioConfig.getBucket(), key)
                            .map(content -> rewriteMasterContent(content, "/stream/public/" + videoId));
                })
                .map(rewritten -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                        .body(rewritten));
    }

    /**
     * PUBLIC: variant playlist proxy
     */
    @GetMapping(value = "/public/{videoId}/{quality}/playlist.m3u8", produces = "application/vnd.apple.mpegurl")
    public Mono<ResponseEntity<String>> proxyPublicVariantPlaylist(
            @PathVariable String videoId,
            @PathVariable String quality) {

        log.info("Public variant playlist request: videoId={}, quality={}", videoId, quality);
        return catalogService.getPublicVideo(videoId)
                .flatMap(v -> {
                    String masterKey = s3Service.extractObjectKeyFromUrl(v.getHlsMasterUrl(), minioConfig.getBucket());
                    if (masterKey == null) {
                        return Mono.error(new IllegalArgumentException("HLS master URL is not available"));
                    }
                    String base = baseHlsPrefix(masterKey);
                    String variantKey = base + "/" + quality + "/playlist.m3u8";
                    return s3Service.getObjectAsString(minioConfig.getBucket(), variantKey)
                            .map(content -> rewriteVariantContent(content, "/stream/public/" + videoId, quality));
                })
                .map(rewritten -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                        .body(rewritten));
    }

    /**
     * PUBLIC: segment proxy
     */
    @GetMapping(value = "/public/{videoId}/{quality}/{segment}.ts", produces = "video/mp2t")
    public Mono<ResponseEntity<Flux<ByteBuffer>>> proxyPublicSegment(
            @PathVariable String videoId,
            @PathVariable String quality,
            @PathVariable String segment) {

        log.info("Public segment request: videoId={}, quality={}, segment={}", videoId, quality, segment);
        return catalogService.getPublicVideo(videoId)
                .flatMap(v -> {
                    String masterKey = s3Service.extractObjectKeyFromUrl(v.getHlsMasterUrl(), minioConfig.getBucket());
                    if (masterKey == null) {
                        return Mono.error(new IllegalArgumentException("HLS master URL is not available"));
                    }
                    String base = baseHlsPrefix(masterKey);
                    String segmentKey = base + "/" + quality + "/" + segment + ".ts";
                    Flux<ByteBuffer> stream = s3Service.streamObject(minioConfig.getBucket(), segmentKey);
                    return Mono.just(ResponseEntity.ok()
                            .contentType(MediaType.valueOf("video/mp2t"))
                            .body(stream));
                });
    }
}
