package ru.practicum.main_service.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main_service.category.model.Category;
import ru.practicum.main_service.category.repository.CategoryRepository;
import ru.practicum.main_service.event.dto.*;
import ru.practicum.main_service.event.enums.EventState;
import ru.practicum.main_service.event.mapper.EventMapper;
import ru.practicum.main_service.event.model.Event;
import ru.practicum.main_service.event.repository.EventRepository;
import ru.practicum.main_service.event.service.EventService;
import ru.practicum.main_service.exception.NotFoundException;
import ru.practicum.main_service.exception.ValidationException;
import ru.practicum.main_service.user.model.User;
import ru.practicum.main_service.user.repository.UserRepository;
import ru.practicum.stats_client.StatsClient;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;

    @Override
    public List<EventShortDto> getEventsByUser(Long userId, Integer from, Integer size) {
        User user = getUserById(userId);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);

        return events.stream()
                .map(eventMapper::toEventShortDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User user = getUserById(userId);
        Category category = getCategoryById(newEventDto.getCategory());

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        Event event = eventMapper.toEvent(newEventDto);
        event.setCategory(category);
        event.setInitiator(user);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);

        Event savedEvent = eventRepository.save(event);
        log.info("Создано новое событие с id: {}", savedEvent.getId());

        return eventMapper.toEventFullDto(savedEvent);
    }

    @Override
    public EventFullDto getEventByUser(Long userId, Long eventId) {
        getUserById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        return eventMapper.toEventFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventRequest updateEvent) {
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

        if (updateEvent.getStateAction() != null) {
            switch (updateEvent.getStateAction()) {
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

        return eventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<EventState> states,
                                               List<Long> categories, LocalDateTime rangeStart,
                                               LocalDateTime rangeEnd, Integer from, Integer size) {
        if (rangeStart == null) rangeStart = LocalDateTime.now().minusYears(100);
        if (rangeEnd == null) rangeEnd = LocalDateTime.now().plusYears(100);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Event> events = eventRepository.findEventsByAdmin(users, states, categories,
                rangeStart, rangeEnd, pageable);

        return events.stream()
                .map(eventMapper::toEventFullDto)
                .collect(Collectors.toList());
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

        if (updateEvent.getStateAction() != null) {
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

        return eventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort,
                                               Integer from, Integer size, HttpServletRequest request) {
        if (rangeStart == null) rangeStart = LocalDateTime.now();
        if (rangeEnd == null) rangeEnd = LocalDateTime.now().plusYears(100);

        if (rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Начальная дата не может быть позже конечной");
        }

        Sort sorting = Sort.by("eventDate").ascending();
        if ("views".equals(sort)) {
            sorting = Sort.by("views").descending();
        }

        Pageable pageable = PageRequest.of(from / size, size, sorting);
        List<Event> events = eventRepository.findEventsPublic(text, categories, paid,
                rangeStart, rangeEnd, pageable);

        // Отправка статистики
        statsClient.hit(request);

        return events.stream()
                .map(eventMapper::toEventShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEventPublic(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventId + " не опубликовано");
        }

        // Отправка статистики
        statsClient.hit(request);

        return eventMapper.toEventFullDto(event);
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + categoryId + " не найдена"));
    }

    private void updateEventFields(Event event, UpdateEventRequest updateEvent) {
        if (updateEvent.getAnnotation() != null) {
            event.setAnnotation(updateEvent.getAnnotation());
        }
        if (updateEvent.getDescription() != null) {
            event.setDescription(updateEvent.getDescription());
        }
        if (updateEvent.getEventDate() != null) {
            event.setEventDate(updateEvent.getEventDate());
        }
        if (updateEvent.getLocation() != null) {
            event.setLocation(eventMapper.toLocation(updateEvent.getLocation()));
        }
        if (updateEvent.getPaid() != null) {
            event.setPaid(updateEvent.getPaid());
        }
        if (updateEvent.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEvent.getParticipantLimit());
        }
        if (updateEvent.getRequestModeration() != null) {
            event.setRequestModeration(updateEvent.getRequestModeration());
        }
        if (updateEvent.getTitle() != null) {
            event.setTitle(updateEvent.getTitle());
        }
        if (updateEvent.getCategory() != null) {
            Category category = getCategoryById(updateEvent.getCategory());
            event.setCategory(category);
        }
    }
}