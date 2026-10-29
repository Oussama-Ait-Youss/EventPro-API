package com.eventpro.api.repository;

import com.eventpro.api.model.ReservationLine;
import com.eventpro.api.model.enums.ReservationStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;

public interface ReservationLineRepository
        extends JpaRepository<ReservationLine, Long> {

    @Query("""
        SELECT COALESCE(SUM(rl.quantity), 0)
        FROM ReservationLine rl
        JOIN rl.reservation r
        WHERE r.client.id = :clientId
        AND rl.ticketCategoryPerEvent.id = :categoryId
        AND (
            r.status = :confirmedStatus
            OR (
                r.status = :pendingStatus
                AND r.expirationDate > :now
            )
        )
    """)
    Long countActiveTicketsByClientAndCategory(
            @Param("clientId") Long clientId,
            @Param("categoryId") Long categoryId,
            @Param("confirmedStatus") ReservationStatus confirmedStatus,
            @Param("pendingStatus") ReservationStatus pendingStatus,
            @Param("now") LocalDateTime now
    );
}