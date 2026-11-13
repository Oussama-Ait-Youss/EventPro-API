package com.eventpro.api.repository;

import com.eventpro.api.dto.reservation.ReservationResponse;
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

    //===========================================================
    //Mise en situation 1 : Requête JPQL, Exception & Controller
    //===========================================================


    @Query("""
        SELECT r
        FROM Reservation r
        JOIN ReservationLine rl ON rl.reservation.id = r.id
        JOIN TicketCategoriesPerEvent ticketCatperEvent ON rl.ticketCategoryPerEvent.id = ticketCatperEvent.id
        JOIN TicketCategory ticket ON ticket.id = ticketCatperEvent.ticketCategory.id
        JOIN Client c ON c.id = r.client.id
        where r.status = :status AND c.email = :email AND ticket.name = :nomCategorie AND rl.quantity = :quantiteMin AND r.event.id = :eventId
    """)
    List<ReservationResponse> ReservationList(
            @Param("status") String status,
            @Param("email") String  email,
            @Param("name") String nomcategorie,
            @Param("quantity") int quantiteMin,
            @Param("eventId") Long eventId
    );


}