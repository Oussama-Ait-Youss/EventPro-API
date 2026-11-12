package com.eventpro.api.repository;

import com.eventpro.api.model.TicketCategoriesPerEvent;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketCategoriesPerEventRepository
        extends JpaRepository<TicketCategoriesPerEvent, Long> {

    // Keep your existing repository methods here.

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketCategoriesPerEvent t WHERE t.id = :id")
    Optional<TicketCategoriesPerEvent> findByIdForUpdate(
            @Param("id") Long id
    );
    boolean existsByEvent_IdAndTicketCategory_Id(Long eventId,Long categoryId);
}