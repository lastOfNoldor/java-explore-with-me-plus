package ru.practicum.main_service.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main_service.category.model.Category;
import ru.practicum.main_service.category.service.CategoryService;
import ru.practicum.main_service.event.dto.EventFullDto;
import ru.practicum.main_service.event.dto.EventShortDto;
import ru.practicum.main_service.event.dto.NewEventDto;
import ru.practicum.main_service.event.dto.param.*;
import ru.practicum.main_service.event.mapper.EventMapper;
import ru.practicum.main_service.event.model.Event;
import ru.practicum.main_service.event.model.EventState;
import ru.practicum.main_service.event.model.StateAction;
import ru.practicum.main_service.event.repository.EventRepository;
import ru.practicum.main_service.exception.NotFoundException;
import ru.practicum.main_service.exception.ValidationException;
import ru.practicum.main_service.user.model.User;
import ru.practicum.main_service.user.service.UserService;
import ru.practicum.stat_client.StatClient;
import ru.practicum.stat_dto.EndpointHitDto;
import ru.practicum.stat_dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryService categoryService;
    private final EventMapper eventMapper;
    private final StatClient statClient;
    private final UserService userService;

    @Override
    public List<EventShortDto> getEventsByUser(EventsByUserParams params) {
        Long userId = params.getUserId();
        getUserById(userId);
        int from = params.getFrom();
        int size = params.getSize();
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsBatch(events);
        Map<Long, Long> viewsMap = getEventsViewsBatch(events);

        return events.stream()
                .map(event -> {
                    Long confirmedRequests = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
                    Long views = viewsMap.getOrDefault(event.getId(), 0L);
                    return eventMapper.toEventShortDto(event, confirmedRequests, views);
                })
                .collect(Collectors.toList());
    }

    private Map<Long, Long> getConfirmedRequestsBatch(List<Event> events) {
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        List<Object[]> results = requestRepository.countConfirmedRequestsByEventIds(eventIds, RequestStatus.CONFIRMED);

        return results.stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> (Long) result[1]
                ));
    }

    private Map<Long, Long> getEventsViewsBatch(List<Event> events) {
        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        LocalDateTime earliestCreated = events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(1));

        List<ViewStatsDto> stats = statClient.getStats(
                earliestCreated,
                LocalDateTime.now(),
                uris,
                false
        );
        return stats.stream()
                .collect(Collectors.toMap(
                        stat -> extractEventIdFromUri(stat.getUri()),
                        ViewStatsDto::getHits
                ));
    }

    private Long extractEventIdFromUri(String uri) {
        String[] parts = uri.split("/");
        return Long.parseLong(parts[parts.length - 1]);
    }



    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User user = getUserById(userId);
        Category category = getCategoryById(newEventDto.getCategory());

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        Event event = eventMapper.toEvent(newEventDto, category, user);

        Event savedEvent = eventRepository.save(event);
        log.info("Создано новое событие с id: {}", savedEvent.getId());

        return eventMapper.toEventFullDto(savedEvent);
    }

    private Long getEventRequests(Event event) {
        return 0L; // requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
    }

    private Long getEventViews(Event event) {
        List<ViewStatsDto> stats = statClient.getStats(
                event.getCreatedOn(),
                LocalDateTime.now(),
                List.of("/events/" + event.getId()),
                false
        );

        Long views = 0L;
        if (!stats.isEmpty()) {
            for (ViewStatsDto stat : stats) {
                if (stat.getUri().equals("/events/" + event.getId())) {
                    views = stat.getHits();
                    break;
                }
            }
        }
        return views;
    }

    @Override
    public EventFullDto getEventByUser(EventByUserRequest request) {
        Long userId = request.getUserId();
        Long eventId = request.getEventId();
        getUserById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
        Long views = getEventViews(event);
        Long eventRequests = getEventRequests(event);
        return eventMapper.toEventFullDto(event,eventRequests,views);
    }


    @Override
    @Transactional
    public EventFullDto updateEventByUser(EventByUserRequest request, UpdateEventRequest updateEvent) {
        Long userId = request.getUserId();
        Long eventId = request.getEventId();
        getUserById(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не принадлежит пользователю");
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new ValidationException("Нельзя редактировать опубликованное событие");
        }

        updateEventFields(event, updateEvent);
        StateAction state = updateEvent.getStateAction();
        if (state != null) {
            if (!state.isUserStateAction()) {
                throw new ValidationException("Передано не корректное действие");
            }
            switch (state) {
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Обновлено событие с id: {}", eventId);
        Long views = getEventViews(updatedEvent);
        Long eventRequests = getEventRequests(updatedEvent);
        return eventMapper.toEventFullDto(updatedEvent,eventRequests,views);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventRequest updateEvent) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (updateEvent.getEventDate() != null &&
                updateEvent.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ValidationException("Дата события должна быть не ранее чем через 1 час от текущего момента");
        }

        updateEventFields(event, updateEvent);
        StateAction state = updateEvent.getStateAction();
        if (state != null) {
            if (!state.isAdminStateAction()) {
                throw new ValidationException("Передано не корректное действие");
            }
            switch (updateEvent.getStateAction()) {
                case PUBLISH_EVENT:
                    if (event.getState() != EventState.PENDING) {
                        throw new ValidationException("Событие можно публиковать только если оно в состоянии ожидания");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ValidationException("Нельзя отклонить опубликованное событие");
                    }
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Администратором обновлено событие с id: {}", eventId);
        Long views = getEventViews(updatedEvent);
        Long eventRequests = getEventRequests(event);
        return eventMapper.toEventFullDto(updatedEvent,eventRequests,views);
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(EventsByAdminParams params) {
        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();
        timeRangeValidation(rangeStart, rangeEnd);
        int from = params.getFrom();
        int size = params.getSize();
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Long> users = params.getUsers();
        List<EventState> states = params.getStates();
        List<Long> categories = params.getCategories();

        List<Event> events = eventRepository.findEventsByAdmin(users, states, categories,
                rangeStart, rangeEnd, pageable);
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsBatch(events);
        Map<Long, Long> viewsMap = getEventsViewsBatch(events);

        return events.stream()
                .map(event -> {
                    Long confirmedRequests = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
                    Long views = viewsMap.getOrDefault(event.getId(), 0L);
                    return eventMapper.toEventFullDto(event, confirmedRequests, views);
                })
                .collect(Collectors.toList());
    }


    @Override
    public List<EventShortDto> getEventsPublic(EventsPublicParams params) {
        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();
        timeRangeValidation(rangeStart, rangeEnd);
        int from = params.getFrom();
        int size = params.getSize();
        String text = params.getText();
        Boolean paid = params.getPaid();
        Boolean onlyAvailable = params.getOnlyAvailable();
        List<Long> categories = params.getCategories();
        Sort sorting = Sort.by("eventDate").ascending();
        Pageable pageable = PageRequest.of(from / size, size, sorting);
        List<Event> events = eventRepository.findEventsPublic(text, categories, paid,
                rangeStart, rangeEnd, onlyAvailable, pageable);
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsBatch(events);
        Map<Long, Long> viewsMap = getEventsViewsBatch(events);

        List<EventShortDto> result = events.stream()
                .map(event -> {
                    Long confirmedRequests = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
                    Long views = viewsMap.getOrDefault(event.getId(), 0L);
                    return eventMapper.toEventShortDto(event, confirmedRequests, views);
                })
                .collect(Collectors.toList());
        String sort = params.getSort();
        if ("views".equals(sort)) {
            result.sort(Comparator.comparing(EventShortDto::getViews).reversed());
        }
        HttpServletRequest request = params.getRequest();
        sendStats(request);
        return result;
    }

    private void timeRangeValidation(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart == null) rangeStart = LocalDateTime.now();
        if (rangeEnd == null) rangeEnd = LocalDateTime.now().plusYears(100);
        if (rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Начальная дата не может быть позже конечной");
        }
    }

    private void sendStats(HttpServletRequest request) {
        final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            EndpointHitDto hitDto = EndpointHitDto.builder()
                    .app("ewm-main-service")
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now().format(FORMATTER))
                    .build();

            statClient.hit(hitDto);
        } catch (Exception e) {
            log.error("Ошибка при отправке статистики: {}", e.getMessage());
        }
    }

    @Override
    public EventFullDto getEventPublic(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventId + " не опубликовано");
        }
        Long eventRequests = getEventRequests(event);
        Long views = getEventViews(event);
        sendStats(request);

        return eventMapper.toEventFullDto(event, eventRequests,views);
    }

    private User getUserById(Long userId) {
        return userService.getEntityById(userId);
    }

    private Category getCategoryById(Long categoryId) {
        return categoryService.getEntityById(categoryId);
    }

    private void updateEventFields(Event event, UpdateEventRequest updateEvent) {
        eventMapper.updateEventFromRequest(updateEvent,event);

        if (updateEvent.getCategory() != null) {
            Category category = getCategoryById(updateEvent.getCategory());
            event.setCategory(category);
        }
    }
}
