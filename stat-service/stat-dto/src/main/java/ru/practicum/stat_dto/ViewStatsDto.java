package ru.practicum.stat_dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewStatsDto {
    @NotBlank(message = "App не может быть пустым")
    @Size(max = 255, message = "App должны быть короче 255 символов")
    private String app;
    @NotBlank(message = "Uri не может быть пустым")
    @Size(max = 512, message = "Uri должны быть короче 512 символов")
    private String uri;
    @Min(value = 0, message = "Hits не может быть меньше нуля")
    private Long hits;
}