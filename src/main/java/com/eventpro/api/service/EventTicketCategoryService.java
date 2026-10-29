package com.eventpro.api.service;

import com.eventpro.api.dto.event.EventTicketCategoryAssignRequest;
import com.eventpro.api.dto.event.EventTicketCategoryResponse;

public interface EventTicketCategoryService {

    EventTicketCategoryResponse assignCategory(
            Long eventId,
            EventTicketCategoryAssignRequest request
    );

}