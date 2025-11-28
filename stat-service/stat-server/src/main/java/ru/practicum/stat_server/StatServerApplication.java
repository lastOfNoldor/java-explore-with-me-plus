package ru.practicum.stat_server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@SpringBootApplication
@RestController
public class StatServerApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(StatServerApplication.class);
        Environment env = app.run(args).getEnvironment();

        String port = env.getProperty("server.port");
        String dbUrl = env.getProperty("spring.datasource.url");

        log.info("Stats Server started successfully on port: {}", port);
        log.info("Database URL: {}", dbUrl);
        log.info("Health check available at: http://localhost:{}/health", port);
    }

    @GetMapping("/health")
    public String health() {
        log.debug("Health check called");
        return "OK";
    }
}