package com.prism.prism_auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PrismAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrismAuthApplication.class, args);
	}

}
