package com.eventpro.api.service;

import com.eventpro.api.dto.promotion.PromotionCreateRequest;
import com.eventpro.api.dto.promotion.PromotionResponse;

public interface PromotionService {
    PromotionResponse createPromotion(Long organizerId,Long eventId,PromotionCreateRequest request);

}
