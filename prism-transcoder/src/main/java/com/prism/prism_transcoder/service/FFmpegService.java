package com.prism.prism_transcoder.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.prism.prism_transcoder.config.FFmpegConfig.FFmpegProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FFmpegService {

    private final FFmpegProperties props;

    public void verifyAvailable() throws IOException, InterruptedException {
        String bin = getBinary();
        // If a specific path is configured, fail fast if the file is missing
        if (!"ffmpeg".equalsIgnoreCase(bin)) {
            Path binPath = Path.of(bin);
            if (!binPath.isAbsolute()) {
                binPath = Path.of(System.getProperty("user.dir")).resolve(binPath).normalize();
            }
            if (Files.notExists(binPath)) {
                throw new IllegalStateException("Cannot find file at '" + binPath.toString()
                        + "'. This usually indicates a missing or moved file.");
            }
            // Use the resolved absolute path
            bin = binPath.toString();
        }
        List<String> cmd = List.of(bin, "-version");
        Path workDirForVerify = null;
        if (!"ffmpeg".equalsIgnoreCase(bin)) {
            try {
                Path p = Path.of(bin);
                workDirForVerify = p.getParent();
            } catch (Exception ignore) {
            }
        }
        int exit = run(cmd, workDirForVerify);
        if (exit != 0) {
            throw new IllegalStateException("ffmpeg not available at '" + bin + "'. Exit code: " + exit);
        }
    }

    public int transcodeVariantHls(Path inputFile, Path outputDir, String quality, int height, String vBitrate,
            String aBitrate) throws IOException, InterruptedException {
        Files.createDirectories(outputDir);
        Path variantDir = outputDir.resolve(quality);
        Files.createDirectories(variantDir);
        Path playlist = variantDir.resolve("playlist.m3u8");

        List<String> cmd = new ArrayList<>();
        cmd.add(getBinary());
        cmd.add("-y");
        cmd.add("-i");
        cmd.add(inputFile.toAbsolutePath().toString());
        cmd.add("-vf");
        cmd.add("scale=-2:" + height);
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-preset");
        cmd.add("veryfast");
        cmd.add("-b:v");
        cmd.add(vBitrate);
        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add("-b:a");
        cmd.add(aBitrate);
        cmd.add("-hls_time");
        cmd.add("6");
        cmd.add("-hls_list_size");
        cmd.add("0");
        cmd.add("-f");
        cmd.add("hls");
        cmd.add(playlist.toAbsolutePath().toString());

        return run(cmd, variantDir);
    }

    public void transcodeMultiVariantHls(Path inputFile, Path outputDir) throws IOException, InterruptedException {
        int e360 = transcodeVariantHls(inputFile, outputDir, "360p", 360, "800k", "96k");
        int e480 = transcodeVariantHls(inputFile, outputDir, "480p", 480, "1400k", "128k");
        int e720 = transcodeVariantHls(inputFile, outputDir, "720p", 720, "2800k", "128k");
        int e1080 = transcodeVariantHls(inputFile, outputDir, "1080p", 1080, "5000k", "192k");
        if (e360 != 0 || e480 != 0 || e720 != 0 || e1080 != 0) {
            throw new IllegalStateException("FFmpeg failed for one or more variants: " + e360 + "," + e480 + "," + e720
                    + "," + e1080);
        }
        writeMasterPlaylist(outputDir);
    }

    public void writeMasterPlaylist(Path outputDir) throws IOException {
        Path master = outputDir.resolve("master.m3u8");
        List<String> lines = List.of(
                "#EXTM3U",
                "#EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360",
                "360p/playlist.m3u8",
                "#EXT-X-STREAM-INF:BANDWIDTH=1400000,RESOLUTION=842x480",
                "480p/playlist.m3u8",
                "#EXT-X-STREAM-INF:BANDWIDTH=2800000,RESOLUTION=1280x720",
                "720p/playlist.m3u8",
                "#EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080",
                "1080p/playlist.m3u8");
        Files.createDirectories(outputDir);
        Files.write(master, lines);
    }

    public void generateThumbnails(Path inputFile, Path outputDir) throws IOException, InterruptedException {
        Files.createDirectories(outputDir);
        // Generate thumbnails at 5 seconds
        run(new ArrayList<>(List.of(getBinary(), "-y", "-ss", "00:00:05", "-i",
                inputFile.toAbsolutePath().toString(), "-vframes", "1", "-vf", "scale=320:-1",
                outputDir.resolve("small.jpg").toString())), outputDir);
        run(new ArrayList<>(List.of(getBinary(), "-y", "-ss", "00:00:05", "-i",
                inputFile.toAbsolutePath().toString(), "-vframes", "1", "-vf", "scale=640:-1",
                outputDir.resolve("medium.jpg").toString())), outputDir);
        run(new ArrayList<>(List.of(getBinary(), "-y", "-ss", "00:00:05", "-i",
                inputFile.toAbsolutePath().toString(), "-vframes", "1", "-vf", "scale=1280:-1",
                outputDir.resolve("large.jpg").toString())), outputDir);
    }

    private String getBinary() {
        if (props.getFfmpegPath() == null || props.getFfmpegPath().isBlank()) {
            return "ffmpeg";
        }
        String configured = props.getFfmpegPath();
        // If it's just "ffmpeg" or "ffprobe", use as-is from PATH
        if ("ffmpeg".equalsIgnoreCase(configured) || "ffprobe".equalsIgnoreCase(configured)) {
            return configured;
        }
        // Otherwise resolve relative paths
        Path p = Path.of(configured);
        if (!p.isAbsolute()) {
            p = Path.of(System.getProperty("user.dir")).resolve(p).normalize();
        }
        return p.toString();
    }

    private String getProbeBinary() {
        // Try to derive ffprobe from ffmpeg path if provided
        if (props.getFfmpegPath() != null && !props.getFfmpegPath().isBlank()) {
            Path ffmpeg = Path.of(getBinary());
            Path dir = ffmpeg.getParent();
            if (dir != null) {
                Path candidate = dir.resolve("ffprobe" + (ffmpeg.toString().endsWith(".exe") ? ".exe" : ""));
                if (Files.exists(candidate)) {
                    return candidate.toAbsolutePath().toString();
                }
            }
        }
        return "ffprobe";
    }

    private int run(List<String> command, Path workDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        if (workDir != null)
            pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);
        log.info("Running FFmpeg: {}", String.join(" ", command));
        Process p = pb.start();
        List<String> output = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                output.add(line);
                log.debug(line);
            }
        }
        int exit = p.waitFor();
        if (exit != 0) {
            log.error("FFmpeg exited with code {}. Output:\n{}", exit, String.join("\n", output));
        } else {
            log.info("FFmpeg exited with code {}", exit);
        }
        return exit;
    }

    public Integer probeDurationSeconds(Path inputFile) throws IOException, InterruptedException {
        List<String> cmd = List.of(getProbeBinary(), "-v", "error", "-show_entries", "format=duration", "-of",
                "default=noprint_wrappers=1:nokey=1", inputFile.toAbsolutePath().toString());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        log.info("Running ffprobe: {}", String.join(" ", cmd));
        Process p = pb.start();
        String value = null;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            value = r.readLine();
        }
        int exit = p.waitFor();
        if (exit != 0 || value == null || value.isBlank()) {
            log.warn("ffprobe failed or returned no duration; exit={}", exit);
            return null;
        }
        try {
            double seconds = Double.parseDouble(value.trim());
            return (int) Math.round(seconds);
        } catch (NumberFormatException nfe) {
            log.warn("Unable to parse ffprobe duration: {}", value);
            return null;
        }
    }
}
