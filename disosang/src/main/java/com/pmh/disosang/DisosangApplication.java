package com.pmh.disosang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

//@EnableJpaAuditing
@SpringBootApplication
public class DisosangApplication {

	public static void main(String[] args) {

		SpringApplication.run(DisosangApplication.class, args);

	}

}
