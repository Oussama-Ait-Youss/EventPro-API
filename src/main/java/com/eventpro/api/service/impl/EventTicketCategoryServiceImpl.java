package com.eventpro.api.service.impl;

import com.eventpro.api.dto.event.EventTicketCategoryAssignRequest;
import com.eventpro.api.dto.event.EventTicketCategoryResponse;

import com.eventpro.api.model.Event;
import com.eventpro.api.model.TicketCategory;
import com.eventpro.api.model.TicketCategoriesPerEvent;

import com.eventpro.api.repository.EventRepository;
import com.eventpro.api.repository.TicketCategoryRepository;
import com.eventpro.api.repository.TicketCategoriesPerEventRepository;

import com.eventpro.api.service.EventTicketCategoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EventTicketCategoryServiceImpl
        implements EventTicketCategoryService {

    private final EventRepository eventRepository;

    private final TicketCategoryRepository ticketCategoryRepository;

    private final TicketCategoriesPerEventRepository eventCategoryRepository;

    @Override
    @Transactional
    public EventTicketCategoryResponse assignCategory(
            Long eventId,
            EventTicketCategoryAssignRequest request
    ) {

        // 1. Find event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));

        // 2. Find global ticket category
        TicketCategory category = ticketCategoryRepository
                .findById(request.getTicketCategoryId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket category not found"
                ));

        // 3. Check duplicate assignment
        boolean alreadyAssigned =
                eventCategoryRepository
                        .existsByEvent_IdAndTicketCategory_Id(
                                eventId,
                                category.getId()
                        );

        if (alreadyAssigned) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This category is already assigned to the event"
            );
        }

        // 4. Validate event capacity
        int assignedQuantity = event.getTicketCategories()
                .stream()
                .mapToInt(TicketCategoriesPerEvent::getTotalQuantity)
                .sum();

        int requestedQuantity = request.getQuantity();

        if ((long) assignedQuantity + requestedQuantity
                > event.getCapacity()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket category quantities exceed event capacity"
            );
        }

        // 5. Create event-specific category
        TicketCategoriesPerEvent eventCategory =
                new TicketCategoriesPerEvent();

        eventCategory.setTicketCategory(category);

        eventCategory.setDescription(
                request.getDescription()
        );

        eventCategory.setUnitPrice(
                request.getPrice()
        );

        eventCategory.setTotalQuantity(
                request.getQuantity()
        );

        eventCategory.setReservedQuantity(0);
        eventCategory.setSoldQuantity(0);

        eventCategory.setMaxQuotaPerClient(
                request.getMaxQuotaPerClient()
        );

        // 6. Synchronize relationship
        event.addTicketCategory(eventCategory);

        // 7. Save association
        TicketCategoriesPerEvent savedCategory =
                eventCategoryRepository.save(eventCategory);

        // 8. Build response
        EventTicketCategoryResponse response =
                new EventTicketCategoryResponse();

        response.setId(savedCategory.getId());

        response.setTicketCategoryId(
                category.getId()
        );

        response.setName(
                category.getName()
        );

        response.setDescription(
                savedCategory.getDescription()
        );

        response.setPrice(
                savedCategory.getUnitPrice()
        );

        response.setQuantity(
                savedCategory.getTotalQuantity()
        );

        response.setReservedQuantity(
                savedCategory.getReservedQuantity()
        );

        response.setSoldQuantity(
                savedCategory.getSoldQuantity()
        );

        response.setMaxQuotaPerClient(
                savedCategory.getMaxQuotaPerClient()
        );

        return response;
    }
}