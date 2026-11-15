package com.eventpro.api.service.impl;

import com.eventpro.api.dto.refund.RefundRequest;
import com.eventpro.api.dto.refund.RefundResponse;
import com.eventpro.api.model.Client;
import com.eventpro.api.model.Payment;
import com.eventpro.api.model.Refund;
import com.eventpro.api.model.Reservation;
import com.eventpro.api.model.enums.PaymentStatus;
import com.eventpro.api.model.enums.RefundStatus;
import com.eventpro.api.repository.PaymentRepository;
import com.eventpro.api.repository.RefundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private RefundServiceImpl refundService;

    @Test
    void shouldCreatePendingRefundForOwnedSuccessfulPayment() {
        Payment payment = successfulPayment();
        RefundRequest request = validRequest();
        request.setReason("  duplicate purchase  ");
        when(paymentRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(payment));
        when(refundRepository.findByPayment_Id(4L)).thenReturn(List.of());
        when(refundRepository.save(any(Refund.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefundResponse response = refundService.requestRefund(7L, 4L, request);

        ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository).save(captor.capture());
        assertEquals(RefundStatus.PENDING, response.getStatus());
        assertEquals("duplicate purchase", captor.getValue().getReason());
        assertSame(payment, captor.getValue().getPayment());
    }

    @Test
    void shouldRejectRefundAmountAbovePaymentAmount() {
        when(paymentRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(successfulPayment()));
        when(refundRepository.findByPayment_Id(4L)).thenReturn(List.of());
        RefundRequest request = validRequest();
        request.setAmount(BigDecimal.valueOf(101));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> refundService.requestRefund(7L, 4L, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(refundRepository, never()).save(any());
    }

    @Test
    void shouldRejectRefundForPaymentOwnedByAnotherClient() {
        when(paymentRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(successfulPayment()));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> refundService.requestRefund(99L, 4L, validRequest())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(refundRepository);
    }

    @Test
    void shouldApproveFullRefundAndMarkPaymentRefunded() {
        Payment payment = successfulPayment();
        payment.setId(4L);
        Refund refund = new Refund();
        refund.setId(12L);
        refund.setPayment(payment);
        refund.setAmount(payment.getAmount());
        refund.setStatus(RefundStatus.PENDING);
        when(refundRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(refund));
        when(paymentRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(payment));
        when(refundRepository.save(refund)).thenReturn(refund);

        RefundResponse response = refundService.approveRefund(12L);

        assertEquals(RefundStatus.APPROVED, response.getStatus());
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        verify(paymentRepository).save(payment);
    }

    @Test
    void shouldNotMarkPaymentRefundedForPartialRefund() {
        Payment payment = successfulPayment();
        payment.setId(4L);
        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmount(BigDecimal.valueOf(20));
        refund.setStatus(RefundStatus.PENDING);
        when(refundRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(refund));
        when(paymentRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(payment));
        when(refundRepository.save(refund)).thenReturn(refund);

        refundService.approveRefund(12L);

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldRejectPendingRefund() {
        Refund refund = new Refund();
        refund.setPayment(successfulPayment());
        refund.setStatus(RefundStatus.PENDING);
        when(refundRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(refund));
        when(refundRepository.save(refund)).thenReturn(refund);

        RefundResponse response = refundService.rejectRefund(12L);

        assertEquals(RefundStatus.REJECTED, response.getStatus());
    }

    @Test
    void shouldListRefundsForExistingPayment() {
        Refund refund = new Refund();
        refund.setId(12L);
        refund.setPayment(successfulPayment());
        when(paymentRepository.existsById(4L)).thenReturn(true);
        when(refundRepository.findByPayment_Id(4L)).thenReturn(List.of(refund));

        List<RefundResponse> responses = refundService.getPaymentRefunds(4L);

        assertEquals(1, responses.size());
        assertEquals(4L, responses.get(0).getPaymentId());
    }

    private RefundRequest validRequest() {
        RefundRequest request = new RefundRequest();
        request.setAmount(BigDecimal.valueOf(40));
        request.setReason("reason");
        return request;
    }

    private Payment successfulPayment() {
        Client client = new Client();
        client.setId(7L);
        Reservation reservation = new Reservation();
        reservation.setClient(client);
        Payment payment = new Payment();
        payment.setId(4L);
        payment.setReservation(reservation);
        payment.setAmount(BigDecimal.valueOf(100));
        payment.setStatus(PaymentStatus.SUCCESS);
        return payment;
    }
}
