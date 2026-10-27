package com.eventpro.api.service.impl;

import com.eventpro.api.dto.event.EventCreateRequest;
import com.eventpro.api.dto.promotion.PromotionCreateRequest;
import com.eventpro.api.dto.promotion.PromotionResponse;
import com.eventpro.api.dto.promotion.PromotionUpdateRequest;
import com.eventpro.api.exception.EventNotFoundException;
import com.eventpro.api.exception.OrganizerNotFound;
import com.eventpro.api.exception.PromotionCodeAlreadyExistsException;
import com.eventpro.api.mapper.EventMapper;
import com.eventpro.api.mapper.PromotionMapper;
import com.eventpro.api.model.Event;
import com.eventpro.api.model.Organizer;
import com.eventpro.api.model.Promotion;
import com.eventpro.api.model.enums.DiscountType;
import com.eventpro.api.repository.EventRepository;
import com.eventpro.api.repository.OrganizerRepository;
import com.eventpro.api.repository.PromotionRepository;
import com.eventpro.api.repository.TicketCategoryRepository;
import com.eventpro.api.service.PromotionService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


@Service
public class PromotionServiceImpl implements PromotionService {


//
//    private final EventRepository eventRepository;
//    private final OrganizerRepository organizerRepository;
//    private final TicketCategoryRepository ticketCategoryRepository;
//    private final EventMapper eventMapper;

    private final PromotionRepository promotionRepository;
    private final PromotionMapper promotionMapper;
    private final EventRepository eventRepository;
    private final OrganizerRepository organizerRepository;

    public PromotionServiceImpl(PromotionRepository promotionRepository, PromotionMapper promotionMapper, EventRepository eventRepository, OrganizerRepository organizerRepository) {
        this.promotionRepository = promotionRepository;
        this.promotionMapper = promotionMapper;
        this.eventRepository = eventRepository;
        this.organizerRepository = organizerRepository;
    }

    @Override
    @Transactional
    public List<PromotionResponse> getAllPromotions() {
        return promotionRepository.findAll()
                .stream()
                .map(promotionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PromotionResponse updatePromotion(
            Long organizerId,
            Long eventId,
            Long promotionId,
            PromotionUpdateRequest request) {

        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Promotion not found"
                        )
                );

        // Verify ownership and event association
        if (!promotion.getOrganizer().getId().equals(organizerId)
                || !promotion.getEvent().getId().equals(eventId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This promotion does not belong to the specified organizer and event"
            );
        }

        String normalizedCode = request.getCode()
                .trim()
                .toUpperCase(Locale.ROOT);

        // Check code uniqueness, excluding current promotion
        if (promotionRepository.existsByCodeIgnoreCaseAndIdNot(
                normalizedCode, promotionId)) {

            throw new PromotionCodeAlreadyExistsException(
                    "Promotion code already exists: " + normalizedCode
            );
        }

        // Validate dates
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Promotion end date must be after start date"
            );
        }

        Event event = promotion.getEvent();

        if (request.getStartDate().isBefore(event.getSalesOpeningDate())
                || request.getEndDate().isAfter(event.getSalesClosingDate())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Promotion dates must be within the ticket sales period"
            );
        }

        // Validate discount
        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getDiscountValue()
                .compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Percentage discount cannot exceed 100%"
            );
        }

        // Usage limit cannot be lower than current usage
        if (request.getUsageLimit() < promotion.getUsageCount()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Usage limit cannot be lower than the current usage count"
            );
        }

        // Update existing promotion
        promotionMapper.updateEntityFromDto(request, promotion);
        promotion.setCode(normalizedCode);

        Promotion updatedPromotion = promotionRepository.save(promotion);

        return promotionMapper.toResponse(updatedPromotion);
    }


    @Override
    public PromotionResponse createPromotion(Long organizerId, Long eventId, PromotionCreateRequest request) {

        //find the event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new EventNotFoundException("Event not found!")
                );
        //find the organizer
        Organizer organizer = organizerRepository.findById(organizerId).orElseThrow(() -> new OrganizerNotFound("organizer not found!"));

        // 3. Verify event ownership
        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This event does not belong to the organizer"
            );
        }

        // 4. Check if event already has a promotion
        if (promotionRepository.existsByEventId(eventId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This event already has a promotion"
            );
        }

        // 5. Normalize promotion code
        String promotionCode = request.getCode()
                .trim()
                .toUpperCase(Locale.ROOT);

        // 6. Check duplicate code
        if (promotionRepository.existsByCodeIgnoreCase(promotionCode)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Promotion code already exists"
            );
        }

        // 7. Validate promotion dates
        validateDates(request, event);

        // 8. Validate discount value
        validateDiscount(request);

        // 9. Convert DTO to entity
        Promotion promotion = promotionMapper.toEntity(request);

        // 10. Assign server-managed fields
        promotion.setOrganizer(organizer);
        promotion.setEvent(event);
        promotion.setCode(promotionCode);
        promotion.setUsageCount(0);

        // 11. Save promotion
        Promotion savedPromotion = promotionRepository.save(promotion);

        // 12. Return response DTO
        return promotionMapper.toResponse(savedPromotion);
    }


    private void validateDates(
            PromotionCreateRequest request,
            Event event) {

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Promotion end date must be after start date"
            );
        }

        if (request.getStartDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Promotion start date cannot be in the past"
            );
        }

        if (request.getStartDate().isBefore(event.getSalesOpeningDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Promotion cannot start before ticket sales open"
            );
        }

        if (request.getEndDate().isAfter(event.getSalesClosingDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Promotion cannot end after ticket sales close"
            );
        }
    }


    private void validateDiscount(PromotionCreateRequest request) {

        if (request.getDiscountType() == DiscountType.PERCENTAGE) {

            if (request.getDiscountValue()
                    .compareTo(BigDecimal.valueOf(100)) > 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Percentage discount cannot exceed 100%"
                );
            }
        }
    }
}
