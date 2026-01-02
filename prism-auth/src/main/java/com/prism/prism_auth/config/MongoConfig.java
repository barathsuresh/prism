package com.prism.prism_auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB Configuration
 * Enables automatic auditing for createdAt and updatedAt fields
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
