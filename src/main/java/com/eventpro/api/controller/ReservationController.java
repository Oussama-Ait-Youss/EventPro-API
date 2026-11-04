package com.eventpro.api.controller;

import com.eventpro.api.dto.reservation.ReservationCreateRequest;
import com.eventpro.api.dto.reservation.ReservationResponse;
import com.eventpro.api.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients/{clientId}/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * Create a reservation for a client.
     */
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @PathVariable Long clientId,
            @Valid @RequestBody ReservationCreateRequest request) {

        ReservationResponse response =
                reservationService.createReservation(clientId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getClientReservations(
            @PathVariable Long clientId) {

        return ResponseEntity.ok(
                reservationService.getClientReservations(clientId)
        );
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservation(
            @PathVariable Long clientId,
            @PathVariable Long reservationId) {

        return ResponseEntity.ok(
                reservationService.getReservation(clientId, reservationId)
        );
    }

    @PatchMapping("/{reservationId}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable Long clientId,
            @PathVariable Long reservationId) {

        return ResponseEntity.ok(
                reservationService.cancelReservation(clientId, reservationId)
        );
    }
}