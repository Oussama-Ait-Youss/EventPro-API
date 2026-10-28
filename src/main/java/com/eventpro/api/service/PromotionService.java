package com.eventpro.api.service;

import com.eventpro.api.dto.promotion.PromotionCreateRequest;
import com.eventpro.api.dto.promotion.PromotionResponse;
import com.eventpro.api.dto.promotion.PromotionUpdateRequest;

import java.util.List;

public interface PromotionService {
    PromotionResponse createPromotion(Long organizerId,Long eventId,PromotionCreateRequest request);
    List<PromotionResponse> getAllPromotions();
    PromotionResponse updatePromotion(
            Long organizerId,
            Long eventId,
            Long promotionId,
            PromotionUpdateRequest request
    );

    List<PromotionResponse> getAllActivePromotions();

    void deletePromotion(Long organizerId, Long promotionId);

}
