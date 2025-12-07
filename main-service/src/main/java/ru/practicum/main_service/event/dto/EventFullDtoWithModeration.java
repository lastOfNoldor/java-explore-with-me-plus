package ru.practicum.main_service.event.dto;

import lombok.*;
import ru.practicum.main_service.category.dto.CategoryDto;
import ru.practicum.main_service.event.model.EventState;
import ru.practicum.main_service.event.model.Location;
import ru.practicum.main_service.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventFullDtoWithModeration extends EventFullDto {

    private List<ModerationCommentDto> moderationComments;

    public EventFullDtoWithModeration(Long id, String annotation, CategoryDto category, Long confirmedRequests,
                                      LocalDateTime createdOn, String description, LocalDateTime eventDate,
                                      UserShortDto initiator, Location location, Boolean paid,
                                      Integer participantLimit, LocalDateTime publishedOn,
                                      Boolean requestModeration, EventState state, String title,
                                      Long views, List<ModerationCommentDto> moderationComments) {
        super(id, annotation, category, confirmedRequests, createdOn, description, eventDate,
                initiator, location, paid, participantLimit, publishedOn, requestModeration,
                state, title, views);
        this.moderationComments = moderationComments;
    }

}
