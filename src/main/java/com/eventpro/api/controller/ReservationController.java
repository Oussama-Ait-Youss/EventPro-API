package com.eventpro.api.controller;

import com.eventpro.api.dto.reservation.ReservationCreateRequest;
import com.eventpro.api.dto.reservation.ReservationResponse;
import com.eventpro.api.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}