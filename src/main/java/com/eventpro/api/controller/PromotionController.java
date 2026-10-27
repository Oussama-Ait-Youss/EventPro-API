package com.eventpro.api.controller;

import com.eventpro.api.dto.promotion.PromotionCreateRequest;
import com.eventpro.api.dto.promotion.PromotionResponse;
import com.eventpro.api.dto.promotion.PromotionUpdateRequest;
import com.eventpro.api.service.PromotionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @PostMapping("/organizers/{organizerId}/events/{eventId}")
    public ResponseEntity<PromotionResponse> createPromotion(
            @PathVariable Long organizerId,
            @PathVariable Long eventId,
            @Valid @RequestBody PromotionCreateRequest request) {

        PromotionResponse response = promotionService.createPromotion(
                organizerId,
                eventId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<PromotionResponse>> getAllPromotions() {
        return ResponseEntity.ok(promotionService.getAllPromotions());
    }

    @PutMapping("/organizers/{organizerId}/events/{eventId}/{promotionId}")
    public ResponseEntity<PromotionResponse> updatePromotion(
            @PathVariable Long organizerId,
            @PathVariable Long eventId,
            @PathVariable Long promotionId,
            @Valid @RequestBody PromotionUpdateRequest request) {

        return ResponseEntity.ok(
                promotionService.updatePromotion(
                        organizerId,
                        eventId,
                        promotionId,
                        request
                )
        );
    }
}