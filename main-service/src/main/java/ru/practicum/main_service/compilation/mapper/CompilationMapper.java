package ru.practicum.main_service.compilation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.main_service.compilation.dto.CompilationDto;
import ru.practicum.main_service.compilation.dto.param.NewCompilationDto;
import ru.practicum.main_service.compilation.model.Compilation;
import ru.practicum.main_service.event.dto.EventShortDto;
import ru.practicum.main_service.event.mapper.EventMapper;
import ru.practicum.main_service.event.model.Event;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = EventMapper.class)
public interface CompilationMapper {

    @Mapping(target = "events", source = "events", qualifiedByName = "eventsToEventShortDtos")
    CompilationDto toDto(Compilation compilation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toEntity(NewCompilationDto dto);

    @Named("eventsToEventShortDtos")
    default Set<EventShortDto> eventsToEventShortDtos(Set<Event> events) {
        if (events == null) {
            return Set.of();
        }
        return events.stream()
                .map(this::eventToEventShortDto)
                .collect(Collectors.toSet());
    }

    EventShortDto eventToEventShortDto(Event event);
}