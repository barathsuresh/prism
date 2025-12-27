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

@Configuration
public class RabbitMQConfig {

    public static final String VIDEO_EXCHANGE = "video.exchange";
    public static final String TRANSCODE_QUEUE = "video.transcode.queue";
    public static final String TRANSCODE_ROUTING_KEY = "video.transcode";
    public static final String DLQ_EXCHANGE = "video.dlx.exchange";
    public static final String DLQ_QUEUE = "video.transcode.dlq";

    @Bean
    public TopicExchange videoExchange() {
        return new TopicExchange(VIDEO_EXCHANGE, true, false);
    }

    @Bean
    public Queue transcodeQueue() {
        return QueueBuilder.durable(TRANSCODE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "video.transcode.failed")
                .build();
    }

    @Bean
    public Binding transcodeBinding(Queue transcodeQueue, TopicExchange videoExchange) {
        return BindingBuilder.bind(transcodeQueue)
                .to(videoExchange)
                .with(TRANSCODE_ROUTING_KEY);
    }

    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(DLQ_EXCHANGE, true, false);
    }

    @Bean
    public Queue dlq() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding dlqBinding(Queue dlq, TopicExchange dlqExchange) {
        return BindingBuilder.bind(dlq)
                .to(dlqExchange)
                .with("video.transcode.failed");
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
