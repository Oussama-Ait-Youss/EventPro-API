package com.eventpro.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "ticket_categories_per_event",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_ticket_category",
                        columnNames = {
                                "event_id",
                                "ticket_category_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TicketCategoriesPerEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_category_id", nullable = false)
    private TicketCategory ticketCategory;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity = 0;

    @Column(name = "max_quota_per_client", nullable = false)
    private Integer maxQuotaPerClient;

    @OneToMany(
            mappedBy = "ticketCategoryPerEvent",
            fetch = FetchType.LAZY
    )
    private List<ReservationLine> reservationLines = new ArrayList<>();

    @OneToMany(
            mappedBy = "ticketCategoryPerEvent",
            fetch = FetchType.LAZY
    )
    private List<Ticket> tickets = new ArrayList<>();

    public int getAvailableQuantity() {
        return totalQuantity - reservedQuantity - soldQuantity;
    }

    public boolean hasAvailableStock(int quantity) {
        return quantity > 0 && getAvailableQuantity() >= quantity;
    }
}