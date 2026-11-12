package com.eventpro.api.model;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservation_lines")
@Getter
@Setter
@NoArgsConstructor
public class ReservationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Parent reservation
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    // Ticket category associated with an event
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_category_per_event_id", nullable = false)
    private TicketCategoriesPerEvent ticketCategoryPerEvent;

    // Number of tickets
    @Column(nullable = false)
    private Integer quantity;

    // Ticket price at the time of reservation
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    // quantity × unitPrice
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    // Tickets generated after payment
    @OneToMany(mappedBy = "reservationLine")
    private List<Ticket> tickets = new ArrayList<>();
}