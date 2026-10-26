package com.eventpro.api.mapper;

import com.eventpro.api.dto.event.EventTicketCategoryResponse;
import com.eventpro.api.model.TicketCategoriesPerEvent;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventTicketCategoryMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "ticketCategoryId", source = "ticketCategory.id")
    @Mapping(target = "name", source = "ticketCategory.name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "price", source = "unitPrice")
    @Mapping(target = "quantity", source = "totalQuantity")
    @Mapping(target = "reservedQuantity", source = "reservedQuantity")
    @Mapping(target = "soldQuantity", source = "soldQuantity")
    @Mapping(target = "maxQuotaPerClient", source = "maxQuotaPerClient")
    EventTicketCategoryResponse toResponse(
            TicketCategoriesPerEvent eventCategory
    );
}