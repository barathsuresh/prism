package com.prism.prism_config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableConfigServer
@Slf4j
public class PrismConfigApplication {

	public static void main(String[] args) {
		log.info("[CONFIG] Starting Prism Config Server");
		SpringApplication.run(PrismConfigApplication.class, args);
		log.info("[CONFIG] Prism Config Server started successfully");
	}

}
