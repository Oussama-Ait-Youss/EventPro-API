package com.eventpro.api.controller;

import com.eventpro.api.dto.event.EventTicketCategoryAssignRequest;
import com.eventpro.api.dto.event.EventTicketCategoryResponse;

import com.eventpro.api.service.EventTicketCategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/ticket-categories")
@RequiredArgsConstructor
public class EventTicketCategoryController {

    private final EventTicketCategoryService eventTicketCategoryService;

    @PostMapping
    public ResponseEntity<EventTicketCategoryResponse> assignCategory(
            @PathVariable Long eventId,
            @Valid @RequestBody EventTicketCategoryAssignRequest request
    ) {

        EventTicketCategoryResponse response =
                eventTicketCategoryService.assignCategory(
                        eventId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}