
package com.eventpro.api.controller;

import com.eventpro.api.dto.payment.PaymentCreateRequest;
import com.eventpro.api.dto.payment.PaymentResponse;
import com.eventpro.api.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // Create a pending payment.
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentCreateRequest request) {

        PaymentResponse response = paymentService.createPayment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Simulate successful payment.
    @PostMapping("/{id}/complete")
    public ResponseEntity<PaymentResponse> completePayment(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                paymentService.completePayment(id)
        );
    }

    // Get payment by ID.
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                paymentService.getPayment(id)
        );
    }

    // Get payments belonging to a reservation.
    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByReservation(
            @PathVariable Long reservationId) {

        return ResponseEntity.ok(
                paymentService.getPaymentsByReservation(reservationId)
        );
    }
}
