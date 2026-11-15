package com.eventpro.api.service.impl;

import com.eventpro.api.dto.payment.PaymentCreateRequest;
import com.eventpro.api.dto.payment.PaymentResponse;
import com.eventpro.api.model.Payment;
import com.eventpro.api.model.Reservation;
import com.eventpro.api.model.ReservationLine;
import com.eventpro.api.model.TicketCategoriesPerEvent;
import com.eventpro.api.model.enums.PaymentStatus;
import com.eventpro.api.model.enums.ReservationStatus;
import com.eventpro.api.repository.PaymentRepository;
import com.eventpro.api.repository.ReservationRepository;
import com.eventpro.api.repository.TicketCategoriesPerEventRepository;
import com.eventpro.api.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private TicketCategoriesPerEventRepository categoryRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void shouldCreatePendingPaymentForUnpaidReservation() {
        Reservation reservation = pendingReservation();
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setReservationId(2L);
        when(reservationRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(reservation));
        when(paymentRepository.existsByReservation_IdAndStatus(2L, PaymentStatus.SUCCESS))
                .thenReturn(false);
        when(paymentRepository.findFirstByReservation_IdAndStatusOrderByIdDesc(
                2L, PaymentStatus.PENDING
        )).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.createPayment(request);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertEquals(BigDecimal.valueOf(40), response.getAmount());
        assertTrue(captor.getValue().getReference().startsWith("PAY-"));
        assertSame(reservation, captor.getValue().getReservation());
    }

    @Test
    void shouldReuseExistingPendingPayment() {
        Reservation reservation = pendingReservation();
        Payment pending = new Payment();
        pending.setId(8L);
        pending.setReference("PAY-existing");
        pending.setReservation(reservation);
        pending.setAmount(BigDecimal.valueOf(40));
        pending.setStatus(PaymentStatus.PENDING);
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setReservationId(2L);
        when(reservationRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(reservation));
        when(paymentRepository.existsByReservation_IdAndStatus(2L, PaymentStatus.SUCCESS))
                .thenReturn(false);
        when(paymentRepository.findFirstByReservation_IdAndStatusOrderByIdDesc(
                2L, PaymentStatus.PENDING
        )).thenReturn(Optional.of(pending));

        PaymentResponse response = paymentService.createPayment(request);

        assertEquals(8L, response.getId());
        assertEquals("PAY-existing", response.getReference());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldRejectPaymentForExpiredReservation() {
        Reservation reservation = pendingReservation();
        reservation.setExpirationDate(LocalDateTime.now().minusSeconds(1));
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setReservationId(2L);
        when(reservationRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(reservation));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> paymentService.createPayment(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void shouldCompletePaymentConfirmReservationAndIssueTickets() {
        Reservation reservation = pendingReservation();
        TicketCategoriesPerEvent lineCategory = new TicketCategoriesPerEvent();
        lineCategory.setId(6L);
        ReservationLine line = new ReservationLine();
        line.setQuantity(2);
        line.setTicketCategoryPerEvent(lineCategory);
        reservation.setLines(List.of(line));

        TicketCategoriesPerEvent lockedCategory = new TicketCategoriesPerEvent();
        lockedCategory.setId(6L);
        lockedCategory.setReservedQuantity(2);
        lockedCategory.setSoldQuantity(1);
        Payment payment = new Payment();
        payment.setId(5L);
        payment.setReference("PAY-5");
        payment.setReservation(reservation);
        payment.setAmount(BigDecimal.valueOf(40));
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(5L)).thenReturn(Optional.of(payment));
        when(reservationRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(payment));
        when(categoryRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(lockedCategory));

        PaymentResponse response = paymentService.completePayment(5L);

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(0, lockedCategory.getReservedQuantity());
        assertEquals(3, lockedCategory.getSoldQuantity());
        verify(ticketRepository, times(2)).save(any());
        verify(paymentRepository).save(payment);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void shouldRejectCompletingPaymentWhenReservationHasExpired() {
        Reservation reservation = pendingReservation();
        reservation.setExpirationDate(LocalDateTime.now().minusSeconds(1));
        Payment payment = new Payment();
        payment.setId(5L);
        payment.setReservation(reservation);
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(5L)).thenReturn(Optional.of(payment));
        when(reservationRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(payment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> paymentService.completePayment(5L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldReturnPaymentById() {
        Payment payment = paymentFor(pendingReservation());
        payment.setId(9L);
        when(paymentRepository.findById(9L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPayment(9L);

        assertEquals(9L, response.getId());
        assertEquals(2L, response.getReservationId());
    }

    @Test
    void shouldReturnPaymentsForExistingReservation() {
        Payment payment = paymentFor(pendingReservation());
        when(reservationRepository.existsById(2L)).thenReturn(true);
        when(paymentRepository.findByReservation_Id(2L)).thenReturn(List.of(payment));

        List<PaymentResponse> responses = paymentService.getPaymentsByReservation(2L);

        assertEquals(1, responses.size());
        assertEquals(2L, responses.get(0).getReservationId());
    }

    private Reservation pendingReservation() {
        Reservation reservation = new Reservation();
        reservation.setId(2L);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setExpirationDate(LocalDateTime.now().plusMinutes(10));
        reservation.setTotalAmount(BigDecimal.valueOf(40));
        return reservation;
    }

    private Payment paymentFor(Reservation reservation) {
        Payment payment = new Payment();
        payment.setReference("PAY-test");
        payment.setReservation(reservation);
        payment.setAmount(BigDecimal.valueOf(40));
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }
}
