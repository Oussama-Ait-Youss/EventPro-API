
package com.eventpro.api.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentCreateRequest {

    @NotNull(message = "Reservation ID is required")
    private Long reservationId;
}
