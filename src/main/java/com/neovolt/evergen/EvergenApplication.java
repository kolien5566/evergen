package com.neovolt.evergen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

/**
 * Evergen Application Entrance
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
public class EvergenApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvergenApplication.class, args);
        log.info("Evergen Application started successfully");
    }
}
