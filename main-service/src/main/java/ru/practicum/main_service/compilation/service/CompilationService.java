package ru.practicum.main_service.compilation.service;

import ru.practicum.main_service.compilation.dto.CompilationDto;
import ru.practicum.main_service.compilation.dto.param.GetCompilationsDto;
import ru.practicum.main_service.compilation.dto.param.NewCompilationDto;
import ru.practicum.main_service.compilation.dto.param.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {

    public List<CompilationDto> getCompilations(GetCompilationsDto params);

    public CompilationDto getCompilationById(Long compId);

    public CompilationDto createCompilation(NewCompilationDto compilationDto);

    public void deleteCompilation(Long compId);

    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest);

}