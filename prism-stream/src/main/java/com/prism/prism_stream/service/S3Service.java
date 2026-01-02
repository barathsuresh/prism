package com.prism.prism_stream.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3AsyncClient s3AsyncClient;

    public Mono<String> getObjectAsString(String bucket, String key) {
        log.debug("[STREAM-SERVICE] Fetching object as string - bucket: {}, key: {}", bucket, key);
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        return Mono.fromFuture(s3AsyncClient.getObject(req, AsyncResponseTransformer.toPublisher()))
                .flatMap((ResponsePublisher<GetObjectResponse> publisher) -> Flux.from(publisher)
                        .reduce(new StringBuilder(), (sb, bb) -> {
                            byte[] bytes = new byte[bb.remaining()];
                            bb.get(bytes);
                            sb.append(new String(bytes, StandardCharsets.UTF_8));
                            return sb;
                        })
                        .map(StringBuilder::toString));
    }

    public Flux<ByteBuffer> streamObject(String bucket, String key) {
        log.debug("[STREAM-SERVICE] Streaming object - bucket: {}, key: {}", bucket, key);
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return Mono.fromFuture(s3AsyncClient.getObject(req, AsyncResponseTransformer.toPublisher()))
                .flatMapMany((ResponsePublisher<GetObjectResponse> publisher) -> Flux.from(publisher));
    }

    /**
     * Extract S3 object key from a MinIO/S3 URL or path.
     */
    public String extractObjectKeyFromUrl(String urlOrPath, String bucketName) {
        if (urlOrPath == null) {
            return null;
        }
        if (urlOrPath.startsWith("s3://")) {
            // s3://bucket/key
            String withoutScheme = urlOrPath.substring(5);
            int slashIdx = withoutScheme.indexOf('/');
            if (slashIdx > 0) {
                return withoutScheme.substring(slashIdx + 1);
            }
        }
        if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
            int bucketStart = urlOrPath.indexOf("/" + bucketName + "/");
            if (bucketStart > 0) {
                return urlOrPath.substring(bucketStart + 1 + bucketName.length()).substring(1);
            }
        }
        return urlOrPath;
    }
}
