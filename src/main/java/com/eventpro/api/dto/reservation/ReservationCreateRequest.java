package com.eventpro.api.dto.reservation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ReservationCreateRequest {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    private String promoCode;

    @NotEmpty(message = "At least one ticket category is required")
    private List<@NotNull @Valid ReservationLineRequest> lines;
}