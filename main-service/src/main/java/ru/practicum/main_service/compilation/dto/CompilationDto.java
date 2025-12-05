package ru.practicum.main_service.compilation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.practicum.main_service.event.dto.EventShortDto;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationDto {
    private Long id;

    @NotBlank(message = "Title не может быть пустым")
    @Size(max = 120, message = "Title должен быть короче 120 символов")
    private String title;

    private Boolean pinned;
    private Set<EventShortDto> events;
}