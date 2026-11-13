package com.eventpro.api.service;

import com.eventpro.api.dto.reservation.ReservationCreateRequest;
import com.eventpro.api.dto.reservation.ReservationResponse;

import java.util.List;

public interface ReservationService {

    ReservationResponse createReservation(
            Long clientId,
            ReservationCreateRequest request
    );

    ReservationResponse getReservation(
            Long clientId,
            Long reservationId
    );

    List<ReservationResponse> getClientReservations(
            Long clientId
    );

    ReservationResponse cancelReservation(
            Long clientId,
            Long reservationId
    );
    void expireReservation(Long reservationId);


    //=========================================================================
    //Mise en situation 1 : Requête JPQL, Exception & Controller Mise en situation 1 : Requête JPQL, Exception & Controller
    //=========================================================================


    List<ReservationResponse> RechercheReservation(
            String status,String email,String nomCategorie,int quantiteMin
    );




}