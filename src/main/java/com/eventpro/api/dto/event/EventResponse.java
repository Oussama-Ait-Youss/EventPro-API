package com.eventpro.api.dto.event;

import com.eventpro.api.model.enums.EventStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class EventResponse {

    private Long id;

    private String title;

    private String description;

    private String location;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private LocalDateTime salesStartDate;

    private LocalDateTime salesEndDate;

    private Integer capacity;

    private EventStatus status;

    private Long organizerId;

    private List<EventTicketCategoryResponse> ticketCategories;
}