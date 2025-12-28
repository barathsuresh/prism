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
    }
}
