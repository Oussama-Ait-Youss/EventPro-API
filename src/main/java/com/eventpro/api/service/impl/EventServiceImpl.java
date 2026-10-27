package com.eventpro.api.service.impl;

import com.eventpro.api.aop.annotation.Auditable;
import com.eventpro.api.dto.event.EventCreateRequest;
import com.eventpro.api.dto.event.EventResponse;
import com.eventpro.api.dto.event.EventTicketCategoryRequest;

import com.eventpro.api.dto.event.EventUpdateRequest;
import com.eventpro.api.exception.EventNotFoundException;
import com.eventpro.api.model.Event;
import com.eventpro.api.model.Organizer;
import com.eventpro.api.model.TicketCategory;
import com.eventpro.api.model.TicketCategoriesPerEvent;
import com.eventpro.api.model.enums.EventStatus;

import com.eventpro.api.mapper.EventMapper;

import com.eventpro.api.repository.EventRepository;
import com.eventpro.api.repository.OrganizerRepository;
import com.eventpro.api.repository.TicketCategoryRepository;

import com.eventpro.api.service.EventService;

import jakarta.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final OrganizerRepository organizerRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(
            EventRepository eventRepository,
            OrganizerRepository organizerRepository,
            TicketCategoryRepository ticketCategoryRepository,
            EventMapper eventMapper) {

        this.eventRepository = eventRepository;
        this.organizerRepository = organizerRepository;
        this.ticketCategoryRepository = ticketCategoryRepository;
        this.eventMapper = eventMapper;
    }

    @Auditable(action = "CREATE", resource = "EVENT")
    @Override
    @Transactional
    public EventResponse createEvent(Long organizerId,EventCreateRequest request) {

        // 1. Validate dates
        validateDates(request);

        // 2. Validate ticket categories
        validateTicketCategories(request);

        // 3. Find organizer
        Organizer organizer = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Organizer not found"
                ));

        // 4. Map request to Event entity
        Event event = eventMapper.toEntity(request);

        // 5. Set server-managed fields
        event.setOrganizer(organizer);
        event.setStatus(EventStatus.DRAFT);

        // 6. Build category associations
        for (EventTicketCategoryRequest categoryRequest
                : request.getTicketCategories()) {

            String categoryName = categoryRequest.getName().trim();

            // Reuse existing category or create a new one
            TicketCategory category = ticketCategoryRepository
                    .findByNameIgnoreCase(categoryName)
                    .orElseGet(() -> {
                        TicketCategory newCategory = new TicketCategory();
                        newCategory.setName(categoryName);

                        return ticketCategoryRepository.save(newCategory);
                    });

            // Create event-specific category
            TicketCategoriesPerEvent eventCategory =
                    new TicketCategoriesPerEvent();

            eventCategory.setTicketCategory(category);

            eventCategory.setDescription(
                    categoryRequest.getDescription()
            );

            eventCategory.setUnitPrice(
                    categoryRequest.getPrice()
            );

            eventCategory.setTotalQuantity(
                    categoryRequest.getQuantity()
            );

            eventCategory.setReservedQuantity(0);
            eventCategory.setSoldQuantity(0);

            eventCategory.setMaxQuotaPerClient(
                    categoryRequest.getMaxQuotaPerClient()
            );

            // Synchronize both sides of the relationship
            event.addTicketCategory(eventCategory);
        }

        // 7. Save event and cascade its categories
        Event savedEvent = eventRepository.save(event);

        // 8. Return response
        return eventMapper.toResponse(savedEvent);
    }

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

        if (request.getSalesStartDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sales start date cannot be in the past"
            );
        }

        if (request.getSalesEndDate().isAfter(request.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket sales must end before the event starts"
            );
        }
    }


    @Override
    @Transactional
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll()
                .stream()
                .map(eventMapper::toResponse)
                .toList();
    }



    @Auditable(action = "UPDATE", resource = "EVENT")
    @Override
    @Transactional
    public EventResponse updateEvent(
            Long organizerId,
            Long eventId,
            EventUpdateRequest request) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new EventNotFoundException("Event not found")
                );

        // Check ownership
        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This event does not belong to the organizer"
            );
        }

        // Validate event dates
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Event end date must be after start date"
            );
        }

        if (!request.getSalesEndDate().isAfter(request.getSalesStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sales end date must be after sales start date"
            );
        }

        if (request.getSalesEndDate().isAfter(request.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket sales must close before the event starts"
            );
        }

        // Preserve category capacity constraints
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

        // Update existing entity
        eventMapper.updateEntityFromDto(request, event);

        Event updatedEvent = eventRepository.save(event);

        return eventMapper.toResponse(updatedEvent);
    }

    private void validateTicketCategories(EventCreateRequest request) {

        Set<String> categoryNames = new HashSet<>();
        long totalQuantity = 0;

        for (EventTicketCategoryRequest category
                : request.getTicketCategories()) {

            String name = category.getName()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            if (!categoryNames.add(name)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate ticket category: " + category.getName()
                );
            }

            if (category.getMaxQuotaPerClient() > category.getQuantity()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Maximum quota per client cannot exceed quantity "
                                + "for category: " + category.getName()
                );
            }

            totalQuantity += category.getQuantity();
        }

        if (totalQuantity > request.getCapacity()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Total ticket quantity cannot exceed event capacity"
            );
        }
    }
}