package ru.practicum.stat_client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.stat_dto.EndpointHitDto;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClient {

    private final RestTemplate restTemplate;
    private final String statsServiceUrl = "http://localhost:9090"; // URL сервиса статистики
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void hit(HttpServletRequest request) {
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app("main-service")
                .uri(request.getRequestURI())
                .ip(getClientIpAddress(request))
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();

        try {
            ResponseEntity<Object> response = restTemplate.postForEntity(
                    statsServiceUrl + "/hit",
                    hitDto,
                    Object.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Статистика успешно отправлена для URI: {}", request.getRequestURI());
            } else {
                log.warn("Не удалось отправить статистику для URI: {}. Status: {}",
                        request.getRequestURI(), response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Ошибка при отправке статистики для URI: {}. Error: {}",
                    request.getRequestURI(), e.getMessage());
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}