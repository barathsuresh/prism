package com.prism.prism_upload.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for Video Processing
 * 
 * RabbitMQ is a message broker - it acts like a post office that receives, stores, and delivers messages.
 * Think of it as a middleman between services:
 * - Upload Service (producer) sends messages to RabbitMQ
 * - Transcoder Service (consumer) receives messages from RabbitMQ
 * 
 * Key Concepts:
 * 1. EXCHANGE: Routes messages to queues based on routing keys (like a mail sorting facility)
 * 2. QUEUE: Stores messages until consumers are ready to process them (like a mailbox)
 * 3. BINDING: Links an exchange to a queue with a routing key (like delivery rules)
 * 4. ROUTING KEY: A label used to route messages from exchange to queue
 * 5. DLQ (Dead Letter Queue): Stores failed messages for later inspection/retry
 */
@Configuration
public class RabbitMQConfig {

    // Exchange name - where messages are first sent
    public static final String VIDEO_EXCHANGE = "video.exchange";
    
    // Main queue - stores transcode jobs waiting to be processed
    public static final String TRANSCODE_QUEUE = "video.transcode.queue";
    
    // Routing key - label used to route messages from exchange to queue
    public static final String TRANSCODE_ROUTING_KEY = "video.transcode";
    
    // Dead Letter Exchange - handles failed messages
    public static final String DLQ_EXCHANGE = "video.dlx.exchange";
    
    // Dead Letter Queue - stores messages that failed processing
    public static final String DLQ_QUEUE = "video.transcode.dlq";

    /**
     * Create the main video exchange
     * 
     * TopicExchange routes messages based on routing key patterns.
     * Messages sent to this exchange will be routed to queues based on their routing key.
     * 
     * @param VIDEO_EXCHANGE - exchange name
     * @param durable (true) - exchange survives broker restart
     * @param autoDelete (false) - exchange is not deleted when last queue unbinds
     */
    @Bean
    public TopicExchange videoExchange() {
        return new TopicExchange(VIDEO_EXCHANGE, true, false);
    }

    /**
     * Create the main transcode queue
     * 
     * This queue stores transcode jobs (messages) that are waiting to be processed.
     * When Upload Service uploads a video, it sends a message to this queue.
     * Transcoder Service listens to this queue and processes messages one by one.
     * 
     * Dead Letter Configuration:
     * - If message processing fails (exception thrown or NACK), message goes to DLQ
     * - DLQ helps debug failed jobs without losing data
     * 
     * @param durable - queue survives broker restart, messages are persisted to disk
     * @param x-dead-letter-exchange - failed messages are sent to this exchange
     * @param x-dead-letter-routing-key - routing key used when sending to DLQ
     */
    @Bean
    public Queue transcodeQueue() {
        return QueueBuilder.durable(TRANSCODE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "video.transcode.failed")
                .build();
    }

    /**
     * Bind the transcode queue to the video exchange
     * 
     * This creates a routing rule:
     * "When a message arrives at videoExchange with routing key 'video.transcode',
     *  deliver it to transcodeQueue"
     * 
     * Flow:
     * 1. Upload Service publishes message to videoExchange with key "video.transcode"
     * 2. RabbitMQ uses this binding to find the right queue
     * 3. Message is placed in transcodeQueue
     * 4. Transcoder Service consumes message from transcodeQueue
     */
    @Bean
    public Binding transcodeBinding(Queue transcodeQueue, TopicExchange videoExchange) {
        return BindingBuilder.bind(transcodeQueue)
                .to(videoExchange)
                .with(TRANSCODE_ROUTING_KEY);
    }

    /**
     * Dead Letter Exchange - handles failed messages
     * 
     * When transcoding fails (exception, NACK, max retries exceeded),
     * the message is sent here instead of being lost.
     */
    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(DLQ_EXCHANGE, true, false);
    }

    /**
     * Dead Letter Queue - stores failed messages
     * 
     * You can inspect these messages to debug why transcoding failed.
     * Messages stay here until manually removed or reprocessed.
     */
    @Bean
    public Queue dlq() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    /**
     * Bind DLQ to DLQ exchange
     * 
     * Failed messages are routed to DLQ with key "video.transcode.failed"
     */
    @Bean
    public Binding dlqBinding(Queue dlq, TopicExchange dlqExchange) {
        return BindingBuilder.bind(dlq)
                .to(dlqExchange)
                .with("video.transcode.failed");
    }

    /**
     * JSON Message Converter
     * 
     * Converts Java objects (TranscodeMessage) to JSON before sending to RabbitMQ.
     * When consumer receives message, it's automatically converted back to Java object.
     * 
     * Without this, RabbitMQ would send messages as raw bytes.
     */
    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * RabbitTemplate - main tool for sending messages
     * 
     * This is like a stamp machine at the post office.
     * You give it a message and address (exchange + routing key),
     * and it sends the message to RabbitMQ.
     * 
     * Usage:
     * rabbitTemplate.convertAndSend(exchange, routingKey, messageObject);
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
