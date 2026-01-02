package com.prism.prism_upload.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(
                        IllegalArgumentException ex, ServerWebExchange exchange) {
                log.warn("[EXCEPTION] Invalid argument - path: {}, message: {}",
                                exchange.getRequest().getPath().value(), ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message(ex.getMessage())
                                .path(exchange.getRequest().getPath().value())
                                .build();
                return ResponseEntity.badRequest().body(error);
        }

        @ExceptionHandler(DuplicateUploadException.class)
        public ResponseEntity<ErrorResponse> handleDuplicateUpload(
                        DuplicateUploadException ex, ServerWebExchange exchange) {
                log.warn("[EXCEPTION] Duplicate upload blocked - path: {}", exchange.getRequest().getPath().value());
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.CONFLICT.value())
                                .error("Conflict")
                                .message(ex.getMessage())
                                .path(exchange.getRequest().getPath().value())
                                .build();
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        @ExceptionHandler(WebClientResponseException.class)
        public ResponseEntity<ErrorResponse> handleWebClientResponse(
                        WebClientResponseException ex, ServerWebExchange exchange) {
                HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
                if (status == null)
                        status = HttpStatus.INTERNAL_SERVER_ERROR;
                log.warn("[EXCEPTION] Downstream service error - status: {}, path: {}", ex.getStatusCode().value(),
                                exchange.getRequest().getPath().value());
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(Instant.now())
                                .status(status.value())
                                .error(status.getReasonPhrase())
                                .message(ex.getMessage())
                                .path(exchange.getRequest().getPath().value())
                                .build();
                return ResponseEntity.status(status).body(error);
        }

        @ExceptionHandler(WebExchangeBindException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(
                        WebExchangeBindException ex, ServerWebExchange exchange) {
                log.warn("[EXCEPTION] Validation failed - path: {}", exchange.getRequest().getPath().value());
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Validation Failed")
                                .message("Invalid request parameters")
                                .path(exchange.getRequest().getPath().value())
                                .build();
                return ResponseEntity.badRequest().body(error);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex, ServerWebExchange exchange) {
                log.error("[EXCEPTION] Unexpected error - path: {}", exchange.getRequest().getPath().value(), ex);
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error("Internal Server Error")
                                .message("An unexpected error occurred")
                                .path(exchange.getRequest().getPath().value())
                                .build();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
}
