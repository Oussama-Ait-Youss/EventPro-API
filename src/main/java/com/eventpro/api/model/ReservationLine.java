package com.eventpro.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "reservation_lines")
@Getter
@Setter
@NoArgsConstructor
public class ReservationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_category_per_event_id", nullable = false)
    private TicketCategoriesPerEvent ticketCategoryPerEvent;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    public BigDecimal calculateSubtotal() {
        if (unitPrice == null || quantity == null || quantity <= 0) {
            return BigDecimal.ZERO;
        }

        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @PrePersist
    @PreUpdate
    protected void calculateSubtotalBeforeSave() {
        this.subtotal = calculateSubtotal();
    }
}