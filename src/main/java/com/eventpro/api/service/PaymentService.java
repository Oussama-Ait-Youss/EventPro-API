package com.eventpro.api.service;


import com.eventpro.api.dto.payment.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse processPayment(
            Long clientId,
            Long reservationId
    );

    PaymentResponse getPayment(Long paymentId);

    List<PaymentResponse> getReservationPayments(Long reservationId);
}