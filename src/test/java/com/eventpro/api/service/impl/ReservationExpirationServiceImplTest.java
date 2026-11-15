package com.eventpro.api.service.impl;

import com.eventpro.api.model.Reservation;
import com.eventpro.api.model.enums.ReservationStatus;
import com.eventpro.api.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationExpirationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationServiceImpl reservationService;

    @InjectMocks
    private ReservationExpirationServiceImpl expirationService;

    @Test
    void shouldExpireEveryPendingReservationReturnedByRepository() {
        Reservation first = new Reservation();
        first.setId(11L);
        Reservation second = new Reservation();
        second.setId(12L);
        when(reservationRepository.findByStatusAndExpirationDateBefore(
                eq(ReservationStatus.PENDING),
                argThat(time -> time != null)
        )).thenReturn(List.of(first, second));

        expirationService.expireReservations();

        verify(reservationService).expireReservation(11L);
        verify(reservationService).expireReservation(12L);
    }

    @Test
    void shouldNotDelegateWhenNoReservationsHaveExpired() {
        when(reservationRepository.findByStatusAndExpirationDateBefore(
                eq(ReservationStatus.PENDING),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        expirationService.expireReservations();

        verifyNoInteractions(reservationService);
    }
}
