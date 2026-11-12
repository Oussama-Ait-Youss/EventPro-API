package com.eventpro.api.service.impl;


import com.eventpro.api.model.Reservation;
import com.eventpro.api.model.enums.ReservationStatus;
import com.eventpro.api.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReservationExpirationServiceImpl {

    private final ReservationRepository reservationRepository;
    private final ReservationServiceImpl reservationService;

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void expireReservations() {

        List<Reservation> expiredReservations =
                reservationRepository
                        .findByStatusAndExpirationDateBefore(
                                ReservationStatus.PENDING,
                                LocalDateTime.now()
                        );

        for (Reservation reservation : expiredReservations) {

            // Release the reserved ticket stock.
            reservationService.expireReservation(
                    reservation.getId()
            );
        }
    }
}
