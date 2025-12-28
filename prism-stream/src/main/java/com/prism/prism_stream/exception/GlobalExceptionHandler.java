package com.prism.prism_stream.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgument(IllegalArgumentException ex,
            ServerWebExchange exchange) {
        log.warn("Bad request on {}: {}", exchange.getRequest().getPath(), ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .path(exchange.getRequest().getPath().toString())
                .build();
        return Mono.just(ResponseEntity.badRequest().body(error));
    }

    @ExceptionHandler(WebClientResponseException.NotFound.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNotFound(WebClientResponseException.NotFound ex,
            ServerWebExchange exchange) {
        log.warn("Video not found on {}: catalog returned 404", exchange.getRequest().getPath());
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message("Video not found or not accessible (check visibility: public endpoint requires PUBLIC videos)")
                .path(exchange.getRequest().getPath().toString())
                .build();
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNoResourceFound(NoResourceFoundException ex,
            ServerWebExchange exchange) {
        log.warn("Endpoint not found: {}", exchange.getRequest().getPath());
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message("Endpoint not found. Use /stream/** for smart proxy endpoints.")
                .path(exchange.getRequest().getPath().toString())
                .build();
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex, ServerWebExchange exchange) {
        log.error("Internal server error on {}", exchange.getRequest().getPath(), ex);
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("An error occurred while processing your request")
                .path(exchange.getRequest().getPath().toString())
                .build();
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }
}
