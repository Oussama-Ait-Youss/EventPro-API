package com.eventpro.api.dto.ticketCategory;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketCategoryCreateRequest {

    @NotBlank(message = "Category name is required")
    private String name;
}