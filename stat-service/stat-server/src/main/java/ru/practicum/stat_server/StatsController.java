package ru.practicum.stat_server;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.stat_dto.EndpointHitDto;
import ru.practicum.stat_dto.ViewStatsDto;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StatsController {

    @PostMapping("/hit")
    public ResponseEntity<Void> hit(@RequestBody EndpointHitDto endpointHitDto) {
        // Пока просто возвращаем 201
        return ResponseEntity.status(201).build();
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getStats(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {
        // Пока возвращаем пустой список
        return List.of();
    }
}