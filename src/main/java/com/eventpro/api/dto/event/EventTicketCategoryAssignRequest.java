package com.eventpro.api.dto.event;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EventTicketCategoryAssignRequest {

    @NotNull(message = "Ticket category ID is required")
    private Long ticketCategoryId;

    private String description;

    @NotNull(message = "Ticket price is required")
    @DecimalMin(
            value = "0.0",
            message = "Price cannot be negative"
    )
    private BigDecimal price;

    @NotNull(message = "Ticket quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;

    @NotNull(message = "Maximum quota per client is required")
    @Min(
            value = 1,
            message = "Maximum quota per client must be at least 1"
    )
    private Integer maxQuotaPerClient;
}