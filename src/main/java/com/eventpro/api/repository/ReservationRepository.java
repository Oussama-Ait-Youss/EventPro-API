package com.eventpro.api.repository;

import com.eventpro.api.model.Reservation;
import com.eventpro.api.model.enums.ReservationStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    // 1. Find all reservations belonging to a client
    List<Reservation> findByClient_Id(Long clientId);

    // 2. Find reservations by status
    List<Reservation> findByStatus(ReservationStatus status);

    // 3. Find expired pending reservations
    List<Reservation> findByStatusAndExpirationDateBefore(
            ReservationStatus status,
            LocalDateTime dateTime
    );

    // 4. Lock a reservation before changing its status
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM Reservation r
        WHERE r.id = :reservationId
    """)
    Optional<Reservation> findByIdForUpdate(
            @Param("reservationId") Long reservationId
    );
}