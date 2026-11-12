package com.eventpro.api.service.impl;

import com.eventpro.api.dto.refund.RefundRequest;
import com.eventpro.api.dto.refund.RefundResponse;
import com.eventpro.api.model.Payment;
import com.eventpro.api.model.Refund;
import com.eventpro.api.model.enums.PaymentStatus;
import com.eventpro.api.model.enums.RefundStatus;
import com.eventpro.api.repository.PaymentRepository;
import com.eventpro.api.repository.RefundRepository;
import com.eventpro.api.service.RefundService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public RefundResponse requestRefund(
            Long clientId,
            Long paymentId,
            RefundRequest request
    ) {
        if (request == null || request.getAmount() == null
                || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund amount must be greater than zero"
            );
        }

        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund reason is required"
            );
        }

        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found"
                ));

        // Check that the payment belongs to this client.
        Long reservationClientId =
                payment.getReservation().getClient().getId();

        if (!reservationClientId.equals(clientId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot request a refund for another client's payment"
            );
        }

        // Only successful payments can be refunded.
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only successful payments can be refunded"
            );
        }

        // Prevent multiple refund requests for the same payment.
        if (!refundRepository.findByPayment_Id(paymentId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A refund request already exists for this payment"
            );
        }

        // The requested refund cannot exceed the amount paid.
        if (request.getAmount().compareTo(payment.getAmount()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund amount cannot exceed the payment amount"
            );
        }

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmount(request.getAmount());
        refund.setReason(request.getReason().trim());
        refund.setStatus(RefundStatus.PENDING);

        return toResponse(refundRepository.save(refund));
    }

    @Override
    @Transactional
    public RefundResponse approveRefund(Long refundId) {

        Refund refund = refundRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Refund not found"
                ));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending refunds can be approved"
            );
        }

        Payment payment = paymentRepository
                .findByIdForUpdate(refund.getPayment().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found"
                ));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment is not eligible for a refund"
            );
        }

        refund.setStatus(RefundStatus.APPROVED);

        // Mark the payment as REFUNDED only when the full amount is refunded.
        if (refund.getAmount().compareTo(payment.getAmount()) == 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }

        return toResponse(refundRepository.save(refund));
    }

    @Override
    @Transactional
    public RefundResponse rejectRefund(Long refundId) {

        Refund refund = refundRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Refund not found"
                ));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending refunds can be rejected"
            );
        }

        refund.setStatus(RefundStatus.REJECTED);

        return toResponse(refundRepository.save(refund));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> getPaymentRefunds(Long paymentId) {

        if (!paymentRepository.existsById(paymentId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Payment not found"
            );
        }

        return refundRepository.findByPayment_Id(paymentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private RefundResponse toResponse(Refund refund) {

        RefundResponse response = new RefundResponse();

        response.setId(refund.getId());
        response.setPaymentId(refund.getPayment().getId());
        response.setAmount(refund.getAmount());
        response.setReason(refund.getReason());
        response.setStatus(refund.getStatus());
        response.setRequestDate(refund.getRequestDate());

        return response;
    }
}