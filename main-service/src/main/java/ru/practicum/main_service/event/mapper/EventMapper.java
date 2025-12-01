package ru.practicum.main_service.event.mapper;

import org.mapstruct.*;
import org.springframework.stereotype.Component;
import ru.practicum.main_service.category.dto.CategoryDto;
import ru.practicum.main_service.category.mapper.CategoryMapper;
import ru.practicum.main_service.category.model.Category;
import ru.practicum.main_service.event.dto.EventFullDto;
import ru.practicum.main_service.event.dto.EventShortDto;
import ru.practicum.main_service.event.dto.NewEventDto;
import ru.practicum.main_service.event.dto.UpdateEventRequest;
import ru.practicum.main_service.event.model.Event;

import ru.practicum.main_service.user.dto.UserShortDto;
import ru.practicum.main_service.user.mapper.UserMapper;
import ru.practicum.main_service.user.model.User;

@Mapper(componentModel = "spring",
        uses = {CategoryMapper.class, UserMapper.class})
public interface EventMapper {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEventFromRequest(UpdateEventRequest request, @MappingTarget Event event);

    public Event toEvent(NewEventDto newEventDto, Category category, User initiator) {
        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(category)
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .initiator(initiator)
                .location(newEventDto.getLocation())
                .paid(newEventDto.getPaid())
                .participantLimit(newEventDto.getParticipantLimit())
                .requestModeration(newEventDto.getRequestModeration())
                .title(newEventDto.getTitle())
                .build();
    }

    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    EventFullDto toEventFullDto(Event event);

    default EventFullDto toEventFullDto(Event event, Long confirmedRequests, Long views) {
        EventFullDto dto = toEventFullDto(event);
        dto.setConfirmedRequests(confirmedRequests);
        dto.setViews(views);
        return dto;
    }

    public EventShortDto toEventShortDto(Event event) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(toCategoryDto(event.getCategory()))
                .confirmedRequests(event.getConfirmedRequests())
                .eventDate(event.getEventDate())
                .initiator(toUserShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(event.getViews())
                .build();
    }


    public CategoryDto toCategoryDto(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryDto(category.getId(), category.getName());
    }

    public UserShortDto toUserShortDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserShortDto(user.getId(), user.getName());
    }
}
