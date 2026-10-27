package com.eventpro.api.dto.event;

import com.eventpro.api.model.enums.EventStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class EventCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @NotNull(message = "Sales start date is required")
    private LocalDateTime salesStartDate;

    @NotNull(message = "Sales end date is required")
    private LocalDateTime salesEndDate;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be greater than zero")
    private Integer capacity;

    @NotNull
    private EventStatus status;

    @NotEmpty(message = "At least one ticket category is required")
    private List<@NotNull @Valid EventTicketCategoryRequest> ticketCategories;
}