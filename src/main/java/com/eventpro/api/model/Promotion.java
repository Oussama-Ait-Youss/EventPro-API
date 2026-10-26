package com.eventpro.api.model;

import com.eventpro.api.model.enums.DiscountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private Organizer organizer;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "usage_limit", nullable = false)
    private Integer usageLimit;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @OneToMany(mappedBy = "promotion", fetch = FetchType.LAZY)
    private List<Reservation> reservations = new ArrayList<>();

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();

        return startDate != null
                && endDate != null
                && !now.isBefore(startDate)
                && !now.isAfter(endDate)
                && usageLimit != null
                && usageCount != null
                && usageCount < usageLimit;
    }

    public BigDecimal calculateDiscount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || !isValid()) {
            return BigDecimal.ZERO;
        }

        if (discountValue == null || discountValue.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;

        if (discountType == DiscountType.PERCENTAGE) {
            discount = amount
                    .multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = discountValue;
        }

        return discount.min(amount);
    }
}