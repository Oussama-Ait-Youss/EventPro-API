
package com.eventpro.api.repository;

import com.eventpro.api.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByReservationLine_Reservation_Id(Long reservationId);

    boolean existsByCode(String code);
}
