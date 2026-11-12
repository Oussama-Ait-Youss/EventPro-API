
package com.eventpro.api.dto.payment;

import com.eventpro.api.model.enums.PaymentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class PaymentResponse {

    private Long id;
    private String reference;
    private Long reservationId;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private PaymentStatus status;
}
