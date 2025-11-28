package ru.practicum.main_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@Slf4j
@SpringBootApplication
public class MainServiceApplication {

    public static void main(String[] args) {
        try {
            SpringApplication app = new SpringApplication(MainServiceApplication.class);
            Environment env = app.run(args).getEnvironment();

            String port = env.getProperty("server.port");
            String dbUrl = env.getProperty("spring.datasource.url");

            log.info("Main Service started successfully on port: {}", port);
            log.info("Database URL: {}", dbUrl);
            log.info("Health check available at: http://localhost:{}/health", port);
        } catch (Exception e) {
            log.error("Failed to start Main Service: {}", e.getMessage(), e);
            throw e;
        }
    }
}