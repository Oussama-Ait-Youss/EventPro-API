package com.eventpro.api.service;

import com.eventpro.api.dto.ticketCategory.TicketCategoryCreateRequest;
import com.eventpro.api.dto.ticketCategory.TicketCategoryResponse;

public interface TicketCategoryService {

    TicketCategoryResponse createCategory(
            TicketCategoryCreateRequest request
    );
}