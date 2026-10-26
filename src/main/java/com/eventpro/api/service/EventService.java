package com.eventpro.api.service;

import com.eventpro.api.dto.event.EventCreateRequest;
import com.eventpro.api.dto.event.EventResponse;

import java.util.List;

public interface EventService {

    EventResponse createEvent(
            Long organizerId,
            EventCreateRequest request
    );
    List<EventResponse> getAllEvents();
}