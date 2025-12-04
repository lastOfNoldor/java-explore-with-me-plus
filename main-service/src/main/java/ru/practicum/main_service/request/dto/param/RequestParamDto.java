package ru.practicum.main_service.request.dto.param;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestParamDto {
    @NotNull
    private Long userId;
    @NotNull
    private Long eventId;
}