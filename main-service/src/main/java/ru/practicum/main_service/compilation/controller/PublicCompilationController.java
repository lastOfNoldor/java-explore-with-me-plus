package ru.practicum.main_service.compilation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main_service.compilation.dto.CompilationDto;
import ru.practicum.main_service.compilation.dto.param.GetCompilationsDto;
import ru.practicum.main_service.compilation.service.CompilationService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/compilations")
public class PublicCompilationController {
    private final CompilationService compilationService;

    @GetMapping
    public List<CompilationDto> getCompilations(@Validated GetCompilationsDto params) {
        log.info("GET /compilations?pinned={}&from={}&size={} - получение подборок",
                params.getPinned(), params.getFrom(), params.getSize());
        return compilationService.getCompilations(params);
    }

    @GetMapping("/{compId}")
    public CompilationDto getCompilationById(@PathVariable Long compId) {
        log.info("GET /compilations/{} - получение подборки по ID", compId);
        return compilationService.getCompilationById(compId);
    }
}