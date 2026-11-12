package com.eventpro.api.controller;

import com.eventpro.api.dto.ticketCategory.TicketCategoryCreateRequest;
import com.eventpro.api.dto.ticketCategory.TicketCategoryResponse;
import com.eventpro.api.service.TicketCategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ticket-categories")
@RequiredArgsConstructor
public class TicketCategoryController {

    private final TicketCategoryService ticketCategoryService;

    @PostMapping
    public ResponseEntity<TicketCategoryResponse> createCategory(
            @Valid @RequestBody TicketCategoryCreateRequest request
    ) {

        TicketCategoryResponse response =
                ticketCategoryService.createCategory(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}