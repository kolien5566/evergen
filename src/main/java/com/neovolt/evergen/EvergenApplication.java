package com.neovolt.evergen;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.neovolt.evergen.service.CommandService;
import com.neovolt.evergen.service.TelemetryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Evergen Application Entrance
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
public class EvergenApplication implements CommandLineRunner {

    private final CommandService commandService;
    private final TelemetryService telemetryService;

    public static void main(String[] args) {
        SpringApplication.run(EvergenApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Launching Evergen Application...");
        commandService.pollQueue();
        telemetryService.pollQueue();
        log.info("Launch successfully");
    }
}
