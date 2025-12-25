package com.prism.prism_config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class PrismConfigApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrismConfigApplication.class, args);
	}

}
