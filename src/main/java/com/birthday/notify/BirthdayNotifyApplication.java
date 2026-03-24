package com.birthday.notify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BirthdayNotifyApplication {

	public static void main(String[] args) {
		SpringApplication.run(BirthdayNotifyApplication.class, args);
	}
}
