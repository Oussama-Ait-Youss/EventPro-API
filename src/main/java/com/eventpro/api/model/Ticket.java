package com.eventpro.api.model;

import com.eventpro.api.model.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "issue_date", nullable = false, updatable = false)
    private LocalDateTime issueDate;

    @Column(name = "check_in_date")
    private LocalDateTime checkInDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.VALID;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_line_id", nullable = false)
    private ReservationLine reservationLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_category_per_event_id", nullable = false)
    private TicketCategoriesPerEvent ticketCategoryPerEvent;

    @PrePersist
    protected void onCreate() {
        if (issueDate == null) {
            issueDate = LocalDateTime.now();
        }

        if (code == null || code.isBlank()) {
            code = UUID.randomUUID().toString();
        }
    }
}