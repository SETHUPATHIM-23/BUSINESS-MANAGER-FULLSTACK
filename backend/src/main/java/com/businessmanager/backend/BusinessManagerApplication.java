package com.businessmanager.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;



@SpringBootApplication
@EnableScheduling
public class BusinessManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BusinessManagerApplication.class, args);
	}

}
