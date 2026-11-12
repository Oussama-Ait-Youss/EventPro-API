package com.eventpro.api.dto.reservation;

import com.eventpro.api.model.enums.ReservationStatus;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ReservationResponse {

    private Long id;

    private Long clientId;

    private Long eventId;

    private ReservationStatus status;

    private LocalDateTime creationDate;

    private LocalDateTime expirationDate;

    private BigDecimal totalAmount;

    private List<ReservationLineResponse> lines;
}