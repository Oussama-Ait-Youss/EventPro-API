package com.eventpro.api.mapper;

import com.eventpro.api.dto.reservation.ReservationResponse;
import com.eventpro.api.dto.reservation.ReservationLineResponse;

import com.eventpro.api.model.Reservation;
import com.eventpro.api.model.ReservationLine;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    // Reservation Entity -> ReservationResponse DTO
    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "eventId", source = "event.id")
    ReservationResponse toResponse(Reservation reservation);

    // ReservationLine Entity -> ReservationLineResponse DTO
    @Mapping(
            target = "eventTicketCategoryId",
            source = "ticketCategoryPerEvent.id"
    )
    @Mapping(
            target = "categoryName",
            source = "ticketCategoryPerEvent.ticketCategory.name"
    )
    ReservationLineResponse toLineResponse(ReservationLine line);
}