package ru.practicum.main_service.compilation.dto.param;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompilationRequest {
    private String title;
    private Boolean pinned;
    private Set<Long> events;
}