
package com.eventpro.api.service.impl;

import com.eventpro.api.dto.payment.PaymentCreateRequest;
import com.eventpro.api.dto.payment.PaymentResponse;

import com.eventpro.api.model.Payment;
import com.eventpro.api.model.Reservation;
import com.eventpro.api.model.ReservationLine;
import com.eventpro.api.model.Ticket;
import com.eventpro.api.model.TicketCategoriesPerEvent;

import com.eventpro.api.model.enums.PaymentStatus;
import com.eventpro.api.model.enums.ReservationStatus;

import com.eventpro.api.repository.PaymentRepository;
import com.eventpro.api.repository.ReservationRepository;
import com.eventpro.api.repository.TicketCategoriesPerEventRepository;
import com.eventpro.api.repository.TicketRepository;

import com.eventpro.api.service.PaymentService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final TicketCategoriesPerEventRepository categoryRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentCreateRequest request) {

        if (request == null || request.getReservationId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reservation ID is required"
            );
        }

        // Lock reservation to prevent concurrent payment creation.
        Reservation reservation = reservationRepository
                .findByIdForUpdate(request.getReservationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reservation not found"
                ));

        // Prevent payment for an already confirmed reservation.
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reservation is not awaiting payment"
            );
        }

        // Check the 10-minute reservation expiration.
        if (reservation.getExpirationDate() == null
                || !reservation.getExpirationDate().isAfter(LocalDateTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reservation has expired"
            );
        }

        // Prevent another payment after successful payment.
        if (paymentRepository.existsByReservation_IdAndStatus(
                reservation.getId(),
                PaymentStatus.SUCCESS)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This reservation has already been paid"
            );
        }

        // Reuse an existing pending payment.
        return paymentRepository
                .findFirstByReservation_IdAndStatusOrderByIdDesc(
                        reservation.getId(),
                        PaymentStatus.PENDING
                )
                .map(this::toResponse)
                .orElseGet(() -> {

                    Payment payment = new Payment();

                    payment.setReference(
                            "PAY-" + UUID.randomUUID()
                    );

                    payment.setReservation(reservation);
                    payment.setAmount(reservation.getTotalAmount());
                    payment.setStatus(PaymentStatus.PENDING);

                    return toResponse(paymentRepository.save(payment));
                });
    }

    @Override
    @Transactional
    public PaymentResponse completePayment(Long paymentId) {

        // First retrieve the payment to identify its reservation.
        Payment initialPayment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found"
                ));

        Long reservationId = initialPayment.getReservation().getId();

        // Keep the same lock order as payment creation.
        Reservation reservation = reservationRepository
                .findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reservation not found"
                ));

        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found"
                ));

        // Ensure the payment still belongs to this reservation.
        if (!payment.getReservation().getId().equals(reservation.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment and reservation do not match"
            );
        }

        // Do not process the same payment twice.
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending payments can be completed"
            );
        }

        // Check reservation status.
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reservation is not awaiting payment"
            );
        }

        // Check expiration again.
        if (reservation.getExpirationDate() == null
                || !reservation.getExpirationDate().isAfter(LocalDateTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reservation has expired"
            );
        }

        // 1. Update ticket stock and generate tickets.
        for (ReservationLine line : reservation.getLines()) {

            TicketCategoriesPerEvent category = categoryRepository
                    .findByIdForUpdate(
                            line.getTicketCategoryPerEvent().getId()
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Ticket category not found"
                    ));

            int quantity = line.getQuantity();

            int reserved = category.getReservedQuantity() == null
                    ? 0
                    : category.getReservedQuantity();

            if (reserved < quantity) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Reserved ticket quantity is inconsistent"
                );
            }

            category.setReservedQuantity(reserved - quantity);

            int sold = category.getSoldQuantity() == null
                    ? 0
                    : category.getSoldQuantity();

            category.setSoldQuantity(sold + quantity);

            // Generate one ticket for each reserved ticket.
            for (int i = 0; i < quantity; i++) {

                Ticket ticket = new Ticket();

                ticket.setReservationLine(line);
                ticket.setTicketCategoryPerEvent(category);

                // Ticket code and issue date are generated by @PrePersist.
                ticketRepository.save(ticket);
            }
        }

        // 2. Mark payment as successful.
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());

        // 3. Confirm the reservation.
        reservation.setStatus(ReservationStatus.CONFIRMED);

        paymentRepository.save(payment);
        reservationRepository.save(reservation);

        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found"
                ));

        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByReservation(Long reservationId) {

        if (!reservationRepository.existsById(reservationId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reservation not found"
            );
        }

        return paymentRepository.findByReservation_Id(reservationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentResponse toResponse(Payment payment) {

        PaymentResponse response = new PaymentResponse();

        response.setId(payment.getId());
        response.setReference(payment.getReference());
        response.setReservationId(payment.getReservation().getId());
        response.setAmount(payment.getAmount());
        response.setPaymentDate(payment.getPaymentDate());
        response.setStatus(payment.getStatus());

        return response;
    }
}
