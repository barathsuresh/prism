package com.prism.prism_discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableEurekaServer
@Slf4j
public class PrismDiscoveryApplication {

	public static void main(String[] args) {
		log.info("[DISCOVERY] Starting Prism Discovery Server");
		SpringApplication.run(PrismDiscoveryApplication.class, args);
		log.info("[DISCOVERY] Prism Discovery Server started successfully");
	}

}
