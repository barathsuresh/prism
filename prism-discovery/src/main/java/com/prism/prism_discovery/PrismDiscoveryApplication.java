package com.prism.prism_discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class PrismDiscoveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrismDiscoveryApplication.class, args);
	}

}
