package com.prism.prism_transcoder.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;

import com.prism.prism_transcoder.config.FFmpegConfig.FFmpegProperties;
import com.prism.prism_transcoder.config.MinIOConfig.MinIOProperties;
import com.prism.prism_transcoder.config.StreamConfig.StreamProperties;
import com.prism.prism_transcoder.dto.HlsVariantDto;
import com.prism.prism_transcoder.dto.TranscodeMessage;
import com.prism.prism_transcoder.dto.UpdateStreamsRequest;
import com.prism.prism_transcoder.model.VideoStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscodeJobService {

        private final FFmpegService ffmpegService;
        private final MinIOStorageService storageService;
        private final FFmpegProperties ffmpegProps;
        private final MinIOProperties minioProps;
        private final StreamProperties streamProps;
        private final CatalogClient catalogClient;

        public void process(TranscodeMessage msg) throws Exception {
                log.info("[TRANSCODER] Starting transcode job - videoId: {}, appId: {}", msg.getVideoId(),
                                msg.getAppId());
                // Set status to PROCESSING at the start
                UpdateStreamsRequest processingUpdate = UpdateStreamsRequest.builder()
                                .status(VideoStatus.PROCESSING)
                                .build();
                catalogClient.updateStreams(msg.getVideoId(), processingUpdate).block();
                log.debug("[TRANSCODER] Status updated to PROCESSING - videoId: {}", msg.getVideoId());

                ffmpegService.verifyAvailable();

                Path workspace = getWorkspace(msg);
                Files.createDirectories(workspace);

                // 1) Download source video to workspace
                Path source = storageService.downloadToTemp(msg.getSourceFilePath(), workspace);
                log.debug("[TRANSCODER] Source video downloaded - videoId: {}", msg.getVideoId());

                // 1b) Probe duration (optional)
                Integer durationSec = null;
                try {
                        durationSec = ffmpegService.probeDurationSeconds(source);
                } catch (Exception e) {
                        log.warn("[TRANSCODER] Duration probe failed - videoId: {}", msg.getVideoId(), e);
                }

                // 2) Transcode to HLS (variants from config)
                Path hlsOut = workspace.resolve("hls_out");
                log.debug("[TRANSCODER] Starting HLS transcode - videoId: {}", msg.getVideoId());
                ffmpegService.transcodeMultiVariantHls(source, hlsOut);

                // 2b) Generate thumbnails
                Path thumbsOut = workspace.resolve("thumbnails_out");
                log.debug("[TRANSCODER] Generating thumbnails - videoId: {}", msg.getVideoId());
                ffmpegService.generateThumbnails(source, thumbsOut);

                // 3) Upload HLS output to MinIO at {storageBasePath}/hls
                String baseKey = msg.getStorageBasePath() + "/hls";
                log.debug("[TRANSCODER] Uploading HLS output - videoId: {}", msg.getVideoId());
                storageService.uploadDirectory(hlsOut, baseKey);

                // 3b) Upload thumbnails to MinIO at {storageBasePath}/thumbnails
                String thumbsKey = msg.getStorageBasePath() + "/thumbnails";
                log.debug("[TRANSCODER] Uploading thumbnails - videoId: {}", msg.getVideoId());
                storageService.uploadDirectory(thumbsOut, thumbsKey);

                // 4) Update catalog with HLS URLs and status
                String baseUrl = (streamProps.getBaseUrl() != null && !streamProps.getBaseUrl().isBlank())
                                ? ensureNoTrailingSlash(streamProps.getBaseUrl()) + "/" + baseKey
                                : ensureNoTrailingSlash(minioProps.getUrl()) + "/" + minioProps.getBucketName() + "/"
                                                + baseKey;
                String masterUrl = baseUrl + "/master.m3u8";
                java.util.List<HlsVariantDto> variantDtos;
                java.util.List<com.prism.prism_transcoder.config.FFmpegConfig.FFmpegProperties.Variant> configured = ffmpegProps
                                .getHls() != null ? ffmpegProps.getHls().getVariants() : null;
                if (configured == null || configured.isEmpty()) {
                        variantDtos = java.util.List.of(
                                        HlsVariantDto.builder().quality("360p").bitrateKbps(800)
                                                        .url(baseUrl + "/360p/playlist.m3u8").build(),
                                        HlsVariantDto.builder().quality("480p").bitrateKbps(1400)
                                                        .url(baseUrl + "/480p/playlist.m3u8").build(),
                                        HlsVariantDto.builder().quality("720p").bitrateKbps(2800)
                                                        .url(baseUrl + "/720p/playlist.m3u8").build(),
                                        HlsVariantDto.builder().quality("1080p").bitrateKbps(5000)
                                                        .url(baseUrl + "/1080p/playlist.m3u8").build());
                } else {
                        variantDtos = new java.util.ArrayList<>();
                        for (var v : configured) {
                                int kbps = estimateKbps(v.getVideoBitrate(), v.getBandwidthKbps());
                                variantDtos.add(HlsVariantDto.builder()
                                                .quality(v.getQuality())
                                                .bitrateKbps(kbps)
                                                .url(baseUrl + "/" + v.getQuality() + "/playlist.m3u8")
                                                .build());
                        }
                }

                UpdateStreamsRequest update = UpdateStreamsRequest.builder()
                                .status(VideoStatus.READY)
                                .hlsMasterUrl(masterUrl)
                                .hlsVariants(variantDtos)
                                .durationSeconds(durationSec)
                                .thumbnails(com.prism.prism_transcoder.dto.ThumbnailsDto.builder()
                                                .smallUrl(ensureNoTrailingSlash(minioProps.getUrl()) + "/"
                                                                + minioProps.getBucketName() + "/"
                                                                + thumbsKey + "/small.jpg")
                                                .mediumUrl(ensureNoTrailingSlash(minioProps.getUrl()) + "/"
                                                                + minioProps.getBucketName() + "/"
                                                                + thumbsKey + "/medium.jpg")
                                                .largeUrl(ensureNoTrailingSlash(minioProps.getUrl()) + "/"
                                                                + minioProps.getBucketName() + "/"
                                                                + thumbsKey + "/large.jpg")
                                                .build())
                                .build();
                // Block to ensure update completes before cleanup/log
                catalogClient.updateStreams(msg.getVideoId(), update).block();
                log.debug("[TRANSCODER] Catalog updated with READY status - videoId: {}", msg.getVideoId());

                // 5) Cleanup temp files (best effort)
                tryDelete(workspace);

                log.info("[TRANSCODER] Transcode job completed - videoId: {}, appId: {}, bucket: {}, key: {}",
                                msg.getVideoId(),
                                msg.getAppId(), minioProps.getBucketName(), baseKey);
        }

        private String ensureNoTrailingSlash(String s) {
                if (s == null)
                        return "";
                return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
        }

        private Path getWorkspace(TranscodeMessage msg) {
                String base = ffmpegProps.getTempDir() != null && !ffmpegProps.getTempDir().isBlank()
                                ? ffmpegProps.getTempDir()
                                : System.getProperty("java.io.tmpdir") + "/prism-transcoder";
                return Paths.get(base, msg.getAppId(), msg.getVideoId());
        }

        private int estimateKbps(String videoBitrate, Integer bandwidthKbps) {
                if (bandwidthKbps != null && bandwidthKbps > 0)
                        return bandwidthKbps;
                if (videoBitrate == null)
                        return 800;
                String s = videoBitrate.trim().toLowerCase();
                try {
                        if (s.endsWith("k")) {
                                return Integer.parseInt(s.substring(0, s.length() - 1));
                        } else if (s.endsWith("m")) {
                                return Integer.parseInt(s.substring(0, s.length() - 1)) * 1000;
                        }
                        return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                        return 800;
                }
        }

        private void tryDelete(Path dir) {
                try {
                        Files.walk(dir)
                                        .sorted((a, b) -> b.compareTo(a))
                                        .forEach(p -> {
                                                try {
                                                        Files.deleteIfExists(p);
                                                } catch (IOException ignored) {
                                                }
                                        });
                } catch (IOException e) {
                        log.warn("[TRANSCODER] Failed to cleanup temp directory", e);
                }
        }
}
