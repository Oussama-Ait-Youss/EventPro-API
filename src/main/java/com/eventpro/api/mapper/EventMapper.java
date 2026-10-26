package com.eventpro.api.mapper;

import com.eventpro.api.dto.event.EventCreateRequest;
import com.eventpro.api.dto.event.EventResponse;
import com.eventpro.api.model.Event;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        uses = EventTicketCategoryMapper.class
)
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "ticketCategories", ignore = true)
    @Mapping(target = "promotion", ignore = true)
    @Mapping(target = "salesOpeningDate", source = "salesStartDate")
    @Mapping(target = "salesClosingDate", source = "salesEndDate")
    Event toEntity(EventCreateRequest request);

    @Mapping(target = "organizerId", source = "organizer.id")
    @Mapping(target = "salesStartDate", source = "salesOpeningDate")
    @Mapping(target = "salesEndDate", source = "salesClosingDate")
    @Mapping(target = "ticketCategories", source = "ticketCategories")
    EventResponse toResponse(Event event);
}