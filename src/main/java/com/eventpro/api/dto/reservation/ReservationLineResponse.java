package com.eventpro.api.dto.reservation;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ReservationLineResponse {

    private Long id;

    private Long eventTicketCategoryId;

    private String categoryName;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotal;
}