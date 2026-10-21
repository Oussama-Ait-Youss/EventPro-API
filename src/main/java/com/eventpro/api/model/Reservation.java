package com.eventpro.api.model;

import com.eventpro.api.model.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status = ReservationStatus.PENDING_PAYMENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(
            mappedBy = "reservation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ReservationLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "reservation", fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }

        if (expirationDate == null) {
            expirationDate = creationDate.plusMinutes(10);
        }
    }

    public void addLine(ReservationLine line) {
        lines.add(line);
        line.setReservation(this);
    }

    public void removeLine(ReservationLine line) {
        lines.remove(line);
        line.setReservation(null);
    }

    public BigDecimal calculateTotal() {
        return lines.stream()
                .map(ReservationLine::calculateSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}