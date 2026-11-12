package com.eventpro.api.controller;

import com.eventpro.api.dto.refund.RefundRequest;
import com.eventpro.api.dto.refund.RefundResponse;
import com.eventpro.api.service.RefundService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    // Request a refund
    @PostMapping("/payments/{paymentId}/refunds")
    public ResponseEntity<RefundResponse> requestRefund(
            @PathVariable Long paymentId,
            @RequestParam Long clientId,
            @Valid @RequestBody RefundRequest request
    ) {
        RefundResponse response = refundService.requestRefund(
                clientId,
                paymentId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Get all refunds for a payment
    @GetMapping("/payments/{paymentId}/refunds")
    public ResponseEntity<List<RefundResponse>> getPaymentRefunds(
            @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(
                refundService.getPaymentRefunds(paymentId)
        );
    }

    // Approve a refund
    @PostMapping("/refunds/{refundId}/approve")
    public ResponseEntity<RefundResponse> approveRefund(
            @PathVariable Long refundId
    ) {
        return ResponseEntity.ok(
                refundService.approveRefund(refundId)
        );
    }

    // Reject a refund
    @PostMapping("/refunds/{refundId}/reject")
    public ResponseEntity<RefundResponse> rejectRefund(
            @PathVariable Long refundId
    ) {
        return ResponseEntity.ok(
                refundService.rejectRefund(refundId)
        );
    }
}