package com.eventpro.api.dto.refund;


import com.eventpro.api.model.enums.RefundStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RefundResponse {

    private Long id;

    private Long paymentId;

    private BigDecimal amount;

    private String reason;

    private RefundStatus status;

    private LocalDateTime requestDate;
}
