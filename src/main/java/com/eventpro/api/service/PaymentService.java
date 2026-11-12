
package com.eventpro.api.service;

import com.eventpro.api.dto.payment.PaymentCreateRequest;
import com.eventpro.api.dto.payment.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse createPayment(PaymentCreateRequest request);

    PaymentResponse completePayment(Long paymentId);

    PaymentResponse getPayment(Long paymentId);

    List<PaymentResponse> getPaymentsByReservation(Long reservationId);
}
