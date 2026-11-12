package com.eventpro.api.mapper;

import com.eventpro.api.dto.promotion.PromotionCreateRequest;
import com.eventpro.api.dto.promotion.PromotionResponse;
import com.eventpro.api.dto.promotion.PromotionUpdateRequest;
import com.eventpro.api.model.Promotion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PromotionMapper {

    // DTO -> Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "usageCount", ignore = true)
    @Mapping(target = "reservations", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntityFromDto(
            PromotionUpdateRequest request,
            @MappingTarget Promotion promotion
    );
    Promotion toEntity(PromotionCreateRequest request);

    // Entity -> Response DTO
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "organizerId", source = "organizer.id")
    PromotionResponse toResponse(Promotion promotion);
}