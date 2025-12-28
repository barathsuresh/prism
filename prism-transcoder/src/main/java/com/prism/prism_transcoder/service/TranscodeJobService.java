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
        // Set status to PROCESSING at the start
        UpdateStreamsRequest processingUpdate = UpdateStreamsRequest.builder()
                .status(VideoStatus.PROCESSING)
                .build();
        catalogClient.updateStreams(msg.getVideoId(), processingUpdate).block();
        log.info("Set status to PROCESSING for videoId={}", msg.getVideoId());

        ffmpegService.verifyAvailable();

        Path workspace = getWorkspace(msg);
        Files.createDirectories(workspace);

        // 1) Download source video to workspace
        Path source = storageService.downloadToTemp(msg.getSourceFilePath(), workspace);

        // 1b) Probe duration (optional)
        Integer durationSec = null;
        try {
            durationSec = ffmpegService.probeDurationSeconds(source);
        } catch (Exception e) {
            log.warn("Duration probe failed for {}", source, e);
        }

        // 2) Transcode to HLS (multi-variant: 360p/480p/720p/1080p)
        Path hlsOut = workspace.resolve("hls_out");
        ffmpegService.transcodeMultiVariantHls(source, hlsOut);

        // 2b) Generate thumbnails
        Path thumbsOut = workspace.resolve("thumbnails_out");
        ffmpegService.generateThumbnails(source, thumbsOut);

        // 3) Upload HLS output to MinIO at {storageBasePath}/hls
        String baseKey = msg.getStorageBasePath() + "/hls";
        storageService.uploadDirectory(hlsOut, baseKey);

        // 3b) Upload thumbnails to MinIO at {storageBasePath}/thumbnails
        String thumbsKey = msg.getStorageBasePath() + "/thumbnails";
        storageService.uploadDirectory(thumbsOut, thumbsKey);

        // 4) Update catalog with HLS URLs and status
        String baseUrl = (streamProps.getBaseUrl() != null && !streamProps.getBaseUrl().isBlank())
                ? ensureNoTrailingSlash(streamProps.getBaseUrl()) + "/" + baseKey
                : ensureNoTrailingSlash(minioProps.getUrl()) + "/" + minioProps.getBucketName() + "/" + baseKey;
        String masterUrl = baseUrl + "/master.m3u8";
        HlsVariantDto v360 = HlsVariantDto.builder().quality("360p").bitrateKbps(800)
                .url(baseUrl + "/360p/playlist.m3u8").build();
        HlsVariantDto v480 = HlsVariantDto.builder().quality("480p").bitrateKbps(1400)
                .url(baseUrl + "/480p/playlist.m3u8").build();
        HlsVariantDto v720 = HlsVariantDto.builder().quality("720p").bitrateKbps(2800)
                .url(baseUrl + "/720p/playlist.m3u8").build();
        HlsVariantDto v1080 = HlsVariantDto.builder().quality("1080p").bitrateKbps(5000)
                .url(baseUrl + "/1080p/playlist.m3u8").build();

        UpdateStreamsRequest update = UpdateStreamsRequest.builder()
                .status(VideoStatus.READY)
                .hlsMasterUrl(masterUrl)
                .hlsVariants(java.util.List.of(v360, v480, v720, v1080))
                .durationSeconds(durationSec)
                .thumbnails(com.prism.prism_transcoder.dto.ThumbnailsDto.builder()
                        .smallUrl(ensureNoTrailingSlash(minioProps.getUrl()) + "/" + minioProps.getBucketName() + "/"
                                + thumbsKey + "/small.jpg")
                        .mediumUrl(ensureNoTrailingSlash(minioProps.getUrl()) + "/" + minioProps.getBucketName() + "/"
                                + thumbsKey + "/medium.jpg")
                        .largeUrl(ensureNoTrailingSlash(minioProps.getUrl()) + "/" + minioProps.getBucketName() + "/"
                                + thumbsKey + "/large.jpg")
                        .build())
                .build();
        // Block to ensure update completes before cleanup/log
        catalogClient.updateStreams(msg.getVideoId(), update).block();

        // 5) Cleanup temp files (best effort)
        tryDelete(workspace);

        log.info("Transcoding complete for videoId={} -> s3://{}/{}", msg.getVideoId(), minioProps.getBucketName(),
                baseKey);
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
            log.warn("Failed to cleanup temp dir {}", dir, e);
        }
    }
}
