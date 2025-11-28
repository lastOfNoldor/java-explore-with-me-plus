package ru.practicum.main_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/ready")
    public String readiness() {
        try (Connection connection = dataSource.getConnection()) {
            jdbcTemplate.execute("SELECT 1");
            return "READY";
        } catch (Exception e) {
            log.error("Readiness check failed: {}", e.getMessage());
            return "NOT_READY: " + e.getMessage();
        }
    }
}