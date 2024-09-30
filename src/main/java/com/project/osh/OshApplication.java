package com.project.osh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class OshApplication {

	public static void main(String[] args) {
		SpringApplication.run(OshApplication.class, args);
	}

}
