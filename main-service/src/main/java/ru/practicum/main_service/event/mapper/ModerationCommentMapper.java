package ru.practicum.main_service.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.practicum.main_service.event.dto.ModerationCommentDto;
import ru.practicum.main_service.event.model.ModerationComment;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ModerationCommentMapper {

    @Mapping(target = "eventId", source = "event.id")
    ModerationCommentDto toDto(ModerationComment comment);

}
