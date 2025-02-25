package com.neovolt.evergen;

import com.neovolt.evergen.service.SqsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
public class EvergenApplication implements CommandLineRunner {

	private final SqsService sqsService;

	public static void main(String[] args) {
		SpringApplication.run(EvergenApplication.class, args);
	}

	@Override
	public void run(String... args) {
		log.info("Starting Evergen Application...");
	}

	@Scheduled(fixedDelay = 1000)
	public void pollMessages() {
		try {
			sqsService.receiveMessages();
		} catch (Exception e) {
			log.error("Error polling messages: {}", e.getMessage());
		}
	}
}
