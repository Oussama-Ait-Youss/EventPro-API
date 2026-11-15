package com.eventpro.api.service.impl;

import com.eventpro.api.dto.promotion.PromotionCreateRequest;
import com.eventpro.api.dto.promotion.PromotionResponse;
import com.eventpro.api.dto.promotion.PromotionUpdateRequest;
import com.eventpro.api.mapper.PromotionMapper;
import com.eventpro.api.model.Event;
import com.eventpro.api.model.Organizer;
import com.eventpro.api.model.Promotion;
import com.eventpro.api.model.enums.DiscountType;
import com.eventpro.api.model.enums.PromotionStatus;
import com.eventpro.api.repository.EventRepository;
import com.eventpro.api.repository.OrganizerRepository;
import com.eventpro.api.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private PromotionMapper promotionMapper;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrganizerRepository organizerRepository;

    @InjectMocks
    private PromotionServiceImpl promotionService;

    @Test
    void shouldCreateActivePromotionWithNormalizedCode() {
        Organizer organizer = organizer();
        Event event = eventOwnedBy(organizer);
        PromotionCreateRequest request = validCreateRequest();
        Promotion mapped = new Promotion();
        PromotionResponse expected = new PromotionResponse();
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        when(organizerRepository.findById(7L)).thenReturn(Optional.of(organizer));
        when(promotionRepository.existsByEventId(5L)).thenReturn(false);
        when(promotionRepository.existsByCodeIgnoreCase("EARLY")).thenReturn(false);
        when(promotionMapper.toEntity(request)).thenReturn(mapped);
        when(promotionRepository.save(mapped)).thenReturn(mapped);
        when(promotionMapper.toResponse(mapped)).thenReturn(expected);

        assertSame(expected, promotionService.createPromotion(7L, 5L, request));

        ArgumentCaptor<Promotion> captor = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).save(captor.capture());
        assertEquals("EARLY", captor.getValue().getCode());
        assertEquals(PromotionStatus.ACTIVE, captor.getValue().getStatus());
        assertEquals(0, captor.getValue().getUsageCount());
        assertSame(event, captor.getValue().getEvent());
        assertSame(organizer, captor.getValue().getOrganizer());
    }

    @Test
    void shouldRejectCreateWhenPercentageDiscountExceedsOneHundred() {
        Organizer organizer = organizer();
        Event event = eventOwnedBy(organizer);
        PromotionCreateRequest request = validCreateRequest();
        request.setDiscountValue(BigDecimal.valueOf(101));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        when(organizerRepository.findById(7L)).thenReturn(Optional.of(organizer));
        when(promotionRepository.existsByEventId(5L)).thenReturn(false);
        when(promotionRepository.existsByCodeIgnoreCase("EARLY")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> promotionService.createPromotion(7L, 5L, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(promotionMapper, never()).toEntity(any());
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void shouldUpdateOwnedPromotion() {
        Organizer organizer = organizer();
        Event event = eventOwnedBy(organizer);
        Promotion promotion = new Promotion();
        promotion.setOrganizer(organizer);
        promotion.setEvent(event);
        promotion.setUsageCount(3);
        PromotionUpdateRequest request = validUpdateRequest();
        when(promotionRepository.findById(11L)).thenReturn(Optional.of(promotion));
        when(promotionRepository.existsByCodeIgnoreCaseAndIdNot("SPRING", 11L)).thenReturn(false);
        when(promotionRepository.save(promotion)).thenReturn(promotion);
        PromotionResponse expected = new PromotionResponse();
        when(promotionMapper.toResponse(promotion)).thenReturn(expected);

        assertSame(expected, promotionService.updatePromotion(7L, 5L, 11L, request));

        verify(promotionMapper).updateEntityFromDto(request, promotion);
        assertEquals("SPRING", promotion.getCode());
        assertEquals(PromotionStatus.ACTIVE, promotion.getStatus());
    }

    @Test
    void shouldRejectUpdateWhenUsageLimitIsBelowCurrentUsage() {
        Organizer organizer = organizer();
        Promotion promotion = new Promotion();
        promotion.setOrganizer(organizer);
        Event event = eventOwnedBy(organizer);
        promotion.setEvent(event);
        promotion.setUsageCount(8);
        PromotionUpdateRequest request = validUpdateRequest();
        request.setUsageLimit(7);
        when(promotionRepository.findById(11L)).thenReturn(Optional.of(promotion));
        when(promotionRepository.existsByCodeIgnoreCaseAndIdNot("SPRING", 11L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> promotionService.updatePromotion(7L, 5L, 11L, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void shouldReturnAllActivePromotions() {
        Promotion promotion = new Promotion();
        PromotionResponse response = new PromotionResponse();
        when(promotionRepository.findByStatus(PromotionStatus.ACTIVE)).thenReturn(List.of(promotion));
        when(promotionMapper.toResponse(promotion)).thenReturn(response);

        assertEquals(List.of(response), promotionService.getAllActivePromotions());
    }

    @Test
    void shouldSoftDeletePromotionOwnedByOrganizer() {
        Organizer organizer = organizer();
        Promotion promotion = new Promotion();
        promotion.setOrganizer(organizer);
        promotion.setStatus(PromotionStatus.ACTIVE);
        when(promotionRepository.findById(11L)).thenReturn(Optional.of(promotion));

        promotionService.deletePromotion(7L, 11L);

        assertEquals(PromotionStatus.CANCELED, promotion.getStatus());
        verify(promotionRepository).save(promotion);
    }

    @Test
    void shouldReturnAllPromotions() {
        Promotion promotion = new Promotion();
        PromotionResponse response = new PromotionResponse();
        when(promotionRepository.findAll()).thenReturn(List.of(promotion));
        when(promotionMapper.toResponse(promotion)).thenReturn(response);

        assertEquals(List.of(response), promotionService.getAllPromotions());
    }

    private PromotionCreateRequest validCreateRequest() {
        LocalDateTime now = LocalDateTime.now();
        PromotionCreateRequest request = new PromotionCreateRequest();
        request.setCode(" early ");
        request.setDiscountType(DiscountType.PERCENTAGE);
        request.setDiscountValue(BigDecimal.valueOf(15));
        request.setStartDate(now.plusDays(2));
        request.setEndDate(now.plusDays(4));
        request.setUsageLimit(100);
        return request;
    }

    private PromotionUpdateRequest validUpdateRequest() {
        LocalDateTime now = LocalDateTime.now();
        PromotionUpdateRequest request = new PromotionUpdateRequest();
        request.setCode(" spring ");
        request.setDiscountType(DiscountType.FIXED_AMOUNT);
        request.setDiscountValue(BigDecimal.TEN);
        request.setStartDate(now.plusDays(2));
        request.setEndDate(now.plusDays(4));
        request.setStatus(PromotionStatus.ACTIVE);
        request.setUsageLimit(10);
        return request;
    }

    private Organizer organizer() {
        Organizer organizer = new Organizer();
        organizer.setId(7L);
        return organizer;
    }

    private Event eventOwnedBy(Organizer organizer) {
        LocalDateTime now = LocalDateTime.now();
        Event event = new Event();
        event.setId(5L);
        event.setOrganizer(organizer);
        event.setSalesOpeningDate(now.plusDays(1));
        event.setSalesClosingDate(now.plusDays(6));
        return event;
    }
}
