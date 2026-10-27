package com.eventpro.api.service.impl;

import com.eventpro.api.aop.annotation.Auditable;

import com.eventpro.api.dto.event.EventCreateRequest;
import com.eventpro.api.dto.event.EventResponse;
import com.eventpro.api.dto.event.EventUpdateRequest;

import com.eventpro.api.exception.EventNotFoundException;

import com.eventpro.api.model.Event;
import com.eventpro.api.model.Organizer;

import com.eventpro.api.mapper.EventMapper;

import com.eventpro.api.repository.EventRepository;
import com.eventpro.api.repository.OrganizerRepository;

import com.eventpro.api.service.EventService;

import jakarta.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final OrganizerRepository organizerRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(
            EventRepository eventRepository,
            OrganizerRepository organizerRepository,
            EventMapper eventMapper
    ) {
        this.eventRepository = eventRepository;
        this.organizerRepository = organizerRepository;
        this.eventMapper = eventMapper;
    }

    // =====================================================
    // CREATE EVENT
    // =====================================================

    @Auditable(action = "CREATE", resource = "EVENT")
    @Override
    @Transactional
    public EventResponse createEvent(
            Long organizerId,
            EventCreateRequest request
    ) {

        // 1. Validate event dates
        validateDates(request);

        // 2. Find organizer
        Organizer organizer = organizerRepository
                .findById(organizerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Organizer not found"
                ));

        // 3. Convert DTO to Event entity
        Event event = eventMapper.toEntity(request);

        // 4. Assign organizer
        event.setOrganizer(organizer);

        // 5. Assign status
        event.setStatus(request.getStatus());

        // 6. Save event
        Event savedEvent = eventRepository.save(event);

        // 7. Return response
        return eventMapper.toResponse(savedEvent);
    }

    // =====================================================
    // VALIDATE EVENT CREATION DATES
    // =====================================================

    private void validateDates(EventCreateRequest request) {

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Event end date must be after start date"
            );
        }

        if (!request.getSalesEndDate()
                .isAfter(request.getSalesStartDate())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sales end date must be after sales start date"
            );
        }

        if (request.getSalesStartDate()
                .isBefore(LocalDateTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sales start date cannot be in the past"
            );
        }

        if (request.getSalesEndDate()
                .isAfter(request.getStartDate())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket sales must end before the event starts"
            );
        }
    }

    // =====================================================
    // GET ALL EVENTS
    // =====================================================

    @Override
    @Transactional
    public List<EventResponse> getAllEvents() {

        return eventRepository.findAll()
                .stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    // =====================================================
    // UPDATE EVENT
    // =====================================================

    @Auditable(action = "UPDATE", resource = "EVENT")
    @Override
    @Transactional
    public EventResponse updateEvent(
            Long organizerId,
            Long eventId,
            EventUpdateRequest request
    ) {

        // 1. Find event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new EventNotFoundException("Event not found")
                );

        // 2. Check ownership
        if (!event.getOrganizer().getId().equals(organizerId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This event does not belong to the organizer"
            );
        }

        // 3. Validate event dates
        if (!request.getEndDate().isAfter(request.getStartDate())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Event end date must be after start date"
            );
        }

        if (!request.getSalesEndDate()
                .isAfter(request.getSalesStartDate())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sales end date must be after sales start date"
            );
        }

        if (request.getSalesEndDate()
                .isAfter(request.getStartDate())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket sales must close before the event starts"
            );
        }

        // 4. Preserve category capacity constraints
        int totalCategoryQuantity = event.getTicketCategories()
                .stream()
                .mapToInt(category -> category.getTotalQuantity())
                .sum();

        if (request.getCapacity() < totalCategoryQuantity) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Event capacity cannot be smaller than total ticket category quantities"
            );
        }

        // 5. Update existing entity
        eventMapper.updateEntityFromDto(request, event);

        // 6. Save
        Event updatedEvent = eventRepository.save(event);

        // 7. Return response
        return eventMapper.toResponse(updatedEvent);
    }
}