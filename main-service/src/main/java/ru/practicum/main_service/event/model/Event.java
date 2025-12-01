package ru.practicum.main_service.event.model;

import jakarta.persistence.*;
import ru.practicum.main_service.category.model.Category;

import java.time.LocalDateTime;

@Entity

public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String title;
    private String annotation;
    private String description;

    private LocalDateTime createdOn;
    private LocalDateTime eventDate;
    private LocalDateTime publishedOn;
    @Enumerated(EnumType.STRING)
    private EventState state;
    private Boolean requestModeration;

    private Integer participantLimit;
    private Boolean paid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    private Long initiatorId;     // id вместо  объектов ?

    private Location location;

    // Эти поля НЕ должны быть в Entity - вычисляются
    // private Long confirmedRequests;
    // private Long views;
}
