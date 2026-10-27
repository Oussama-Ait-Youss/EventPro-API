package com.eventpro.api.repository;

import com.eventpro.api.model.TicketCategoriesPerEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCategoriesPerEventRepository
        extends JpaRepository<TicketCategoriesPerEvent, Long> {
    boolean existsByEvent_IdAndTicketCategory_Id(
            Long eventId,
            Long ticketCategoryId
    );
}