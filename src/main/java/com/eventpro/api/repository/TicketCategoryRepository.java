package com.eventpro.api.repository;

import com.eventpro.api.model.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketCategoryRepository
        extends JpaRepository<TicketCategory, Long> {

    Optional<TicketCategory> findByNameIgnoreCase(String name);
}