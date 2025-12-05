package ru.practicum.main_service.compilation.dto.param;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {
    @NotBlank(message = "Title не может быть пустым")
    @Size(max = 120, message = "Title должен быть короче 120 символов")
    private String title;

    private Boolean pinned = false;
    private Set<Long> events;
}