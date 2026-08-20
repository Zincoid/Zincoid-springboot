package com.zincoid.me;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ZincoidApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZincoidApplication.class, args);
	}
}
