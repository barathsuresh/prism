package com.prism.prism_transcoder.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        // TODO(concurrency): Tune consumer concurrency and prefetch once we decide
        // desired parallelism. Options:
        // - factory.setConcurrentConsumers(N)
        // - factory.setMaxConcurrentConsumers(M)
        // - factory.setPrefetchCount(P)
        // Prefer wiring these from Config Server properties so they can be
        // environment-specific (dev/stage/prod) and adjusted without redeploy.
        // For now, we keep a single consumer per instance to avoid FFmpeg CPU
        // contention.
        // No message converter: listener consumes byte[] and uses ObjectMapper manually
        return factory;
    }
}
