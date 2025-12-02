package ru.practicum.main_service.event.mapper;

import org.mapstruct.*;
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

    EventFullDto toEventFullDto(Event event);


    default EventFullDto toEventFullDto(Event event, Long confirmedRequests, Long views) {
        EventFullDto dto = toEventFullDto(event);
        if (confirmedRequests != null) {
            dto.setConfirmedRequests(confirmedRequests);
        }
        if (views != null) {
            dto.setViews(views);
        }
        return dto;
    }

    EventShortDto toEventShortDto(Event event);

    default EventShortDto toEventShortDto(Event event, Long confirmedRequests, Long views) {
        EventShortDto dto = toEventShortDto(event);
        if (confirmedRequests != null) {
            dto.setConfirmedRequests(confirmedRequests);
        }
        if (views != null) {
            dto.setViews(views);
        }
        return dto;
    }

    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiator", source = "initiator")
    Event toEvent(NewEventDto newEventDto, Category category, User initiator);


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
