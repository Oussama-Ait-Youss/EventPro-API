package com.eventpro.api.dto.event;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EventTicketCategoryResponse {

    private Long id;

    private Long ticketCategoryId;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer quantity;

    private Integer reservedQuantity;

    private Integer soldQuantity;

    private Integer maxQuotaPerClient;
}