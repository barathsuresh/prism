package com.prism.prism_transcoder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
public class FFmpegConfig {

    @Bean
    @ConfigurationProperties(prefix = "transcoder")
    public FFmpegProperties ffmpegProperties() {
        return new FFmpegProperties();
    }

    @Data
    public static class FFmpegProperties {
        /** Path to ffmpeg executable; if empty, uses ffmpeg from PATH */
        private String ffmpegPath = "ffmpeg";
        /** Temporary working directory for transcoding */
        private String tempDir;
        /**
         * Optional HLS variants configuration. If empty, defaults will be used.
         * Example in YAML:
         * transcoder:
         * hls:
         * variants:
         * - quality: 144p
         * height: 144
         * videoBitrate: 200k
         * audioBitrate: 64k
         * bandwidthKbps: 200
         */
        private Hls hls;

        @Data
        public static class Hls {
            private java.util.List<Variant> variants;
        }

        @Data
        public static class Variant {
            private String quality; // e.g., "144p"
            private Integer height; // e.g., 144
            private String videoBitrate;// e.g., "200k"
            private String audioBitrate;// e.g., "64k"
            private Integer bandwidthKbps; // optional: for master manifest
            private Integer width; // optional: if omitted, assume 16:9 from height
        }
    }
}
