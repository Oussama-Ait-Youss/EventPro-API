package com.eventpro.api.service.impl;

import com.eventpro.api.dto.ticketCategory.TicketCategoryCreateRequest;
import com.eventpro.api.dto.ticketCategory.TicketCategoryResponse;
import com.eventpro.api.model.TicketCategory;
import com.eventpro.api.repository.TicketCategoryRepository;
import com.eventpro.api.service.TicketCategoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TicketCategoryServiceImpl
        implements TicketCategoryService {

    private final TicketCategoryRepository ticketCategoryRepository;

    @Override
    @Transactional
    public TicketCategoryResponse createCategory(
            TicketCategoryCreateRequest request
    ) {

        // 1. Normalize category name
        String name = request.getName().trim();

        // 2. Check if category already exists
        if (ticketCategoryRepository
                .findByNameIgnoreCase(name)
                .isPresent()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ticket category already exists"
            );
        }

        // 3. Create category
        TicketCategory category = new TicketCategory();

        category.setName(name);

        // 4. Save
        TicketCategory savedCategory =
                ticketCategoryRepository.save(category);

        // 5. Build response
        TicketCategoryResponse response =
                new TicketCategoryResponse();

        response.setId(savedCategory.getId());
        response.setName(savedCategory.getName());

        return response;
    }
}