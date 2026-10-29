package com.eventpro.api.service.impl;

import com.eventpro.api.aop.annotation.Auditable;
import com.eventpro.api.dto.reservation.ReservationCreateRequest;
import com.eventpro.api.dto.reservation.ReservationLineRequest;
import com.eventpro.api.dto.reservation.ReservationResponse;
import com.eventpro.api.mapper.ReservationMapper;
import com.eventpro.api.model.Client;
import com.eventpro.api.model.Event;
import com.eventpro.api.model.Promotion;
import com.eventpro.api.model.Reservation;
import com.eventpro.api.model.ReservationLine;
import com.eventpro.api.model.TicketCategoriesPerEvent;
import com.eventpro.api.model.enums.EventStatus;
import com.eventpro.api.model.enums.ReservationStatus;
import com.eventpro.api.repository.ClientRepository;
import com.eventpro.api.repository.EventRepository;
import com.eventpro.api.repository.PromotionRepository;
import com.eventpro.api.repository.ReservationLineRepository;
import com.eventpro.api.repository.ReservationRepository;
import com.eventpro.api.repository.TicketCategoriesPerEventRepository;
import com.eventpro.api.service.ReservationService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationLineRepository reservationLineRepository;
    private final ClientRepository clientRepository;
    private final EventRepository eventRepository;
    private final TicketCategoriesPerEventRepository categoryRepository;
    private final PromotionRepository promotionRepository;
    private final ReservationMapper reservationMapper;

    public ReservationServiceImpl(
            ReservationRepository reservationRepository,
            ReservationLineRepository reservationLineRepository,
            ClientRepository clientRepository,
            EventRepository eventRepository,
            TicketCategoriesPerEventRepository categoryRepository,
            PromotionRepository promotionRepository,
            ReservationMapper reservationMapper) {

        this.reservationRepository = reservationRepository;
        this.reservationLineRepository = reservationLineRepository;
        this.clientRepository = clientRepository;
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.promotionRepository = promotionRepository;
        this.reservationMapper = reservationMapper;
    }

    @Override
    @Auditable(action = "CREATE", resource = "RESERVATION")
    @Transactional
    public ReservationResponse createReservation(
            Long clientId,
            ReservationCreateRequest request) {

        // 1. Validate the request.
        if (request == null || request.getEventId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Event ID is required"
            );
        }

        List<ReservationLineRequest> requestedLines = request.getLines();

        if (requestedLines == null || requestedLines.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one ticket category is required"
            );
        }

        // Validate quantities and prevent duplicate categories.
        Set<Long> categoryIds = new HashSet<>();

        for (ReservationLineRequest line : requestedLines) {

            if (line == null
                    || line.getEventTicketCategoryId() == null
                    || line.getQuantity() == null
                    || line.getQuantity() <= 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Each reservation line must have a valid category ID and positive quantity"
                );
            }

            if (!categoryIds.add(line.getEventTicketCategoryId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate ticket categories are not allowed"
                );
            }
        }

        // 2. Find and lock the client.
        Client client = clientRepository.findByIdForUpdate(clientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Client not found"
                ));

        // 3. Find the event.
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only published events accept reservations"
            );
        }

        // 4. Validate the ticket sales period.
        LocalDateTime now = LocalDateTime.now();

        if (event.getSalesOpeningDate() == null
                || event.getSalesClosingDate() == null
                || now.isBefore(event.getSalesOpeningDate())
                || now.isAfter(event.getSalesClosingDate())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket sales are not open"
            );
        }

        // 5. Sort category IDs to help maintain a consistent lock order.
        List<ReservationLineRequest> sortedLines =
                new ArrayList<>(requestedLines);

        sortedLines.sort(
                Comparator.comparing(
                        ReservationLineRequest::getEventTicketCategoryId
                )
        );

        // 6. Initialize the reservation.
        Reservation reservation = new Reservation();

        reservation.setClient(client);
        reservation.setEvent(event);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setCreationDate(now);
        reservation.setExpirationDate(now.plusMinutes(10));

        BigDecimal subtotal = BigDecimal.ZERO;

        // 7. Validate categories, stock, quotas, and calculate line totals.
        for (ReservationLineRequest lineRequest : sortedLines) {

            Long categoryId = lineRequest.getEventTicketCategoryId();
            int quantity = lineRequest.getQuantity();

            TicketCategoriesPerEvent category =
                    categoryRepository.findByIdForUpdate(categoryId)
                            .orElseThrow(() ->
                                    new ResponseStatusException(
                                            HttpStatus.NOT_FOUND,
                                            "Ticket category not found: " + categoryId
                                    )
                            );

            // Ensure the category belongs to the requested event.
            if (category.getEvent() == null
                    || !category.getEvent().getId().equals(event.getId())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Ticket category does not belong to this event"
                );
            }

            // Check available ticket stock.
            if (!category.hasAvailableStock(quantity)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Not enough available tickets for category " + categoryId
                );
            }

            // Check the client's active ticket quota.
            Long existingQuantity =
                    reservationLineRepository.countActiveTicketsByClientAndCategory(
                            clientId,
                            categoryId,
                            ReservationStatus.CONFIRMED,
                            ReservationStatus.PENDING,
                            now
                    );

            if (existingQuantity == null) {
                existingQuantity = 0L;
            }

            if (existingQuantity + quantity
                    > category.getMaxQuotaPerClient()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Maximum ticket quota per client exceeded for category "
                                + categoryId
                );
            }

            // Validate the category price.
            BigDecimal unitPrice = category.getUnitPrice();

            if (unitPrice == null || unitPrice.signum() < 0) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Invalid ticket category price"
                );
            }

            // Calculate the reservation line subtotal.
            BigDecimal lineSubtotal = unitPrice.multiply(
                    BigDecimal.valueOf(quantity)
            );

            subtotal = subtotal.add(lineSubtotal);

            // Reserve the requested stock.
            Integer reservedQuantity = category.getReservedQuantity();

            if (reservedQuantity == null) {
                reservedQuantity = 0;
            }

            category.setReservedQuantity(reservedQuantity + quantity);

            // Create the reservation line.
            ReservationLine reservationLine = new ReservationLine();

            reservationLine.setTicketCategoryPerEvent(category);
            reservationLine.setQuantity(quantity);
            reservationLine.setUnitPrice(unitPrice);
            reservationLine.setSubtotal(lineSubtotal);

            reservation.addLine(reservationLine);
        }

        // 8. Validate the optional promo code.
        Promotion promotion = null;
        BigDecimal discountAmount = BigDecimal.ZERO;

        String promoCode = request.getPromoCode();

        if (promoCode != null && !promoCode.isBlank()) {

            String normalizedCode = promoCode.trim();

            promotion = promotionRepository
                    .findByCodeIgnoreCase(normalizedCode)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Invalid promo code"
                    ));

            // Validate the promotion's active period.
            if (promotion.getStartDate() == null
                    || promotion.getEndDate() == null
                    || now.isBefore(promotion.getStartDate())
                    || now.isAfter(promotion.getEndDate())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Promo code is expired or not yet active"
                );
            }

            // Validate the promotion usage limit.
            if (promotion.getUsageLimit() != null
                    && promotion.getUsageCount() != null
                    && promotion.getUsageCount()
                    >= promotion.getUsageLimit()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Promo code usage limit has been reached"
                );
            }

            // Ensure the promotion belongs to this event.
            if (promotion.getEvent() == null
                    || !promotion.getEvent().getId().equals(event.getId())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Promo code does not apply to this event"
                );
            }

            // Calculate the discount using the Promotion entity.
            discountAmount = promotion.calculateDiscount(subtotal);

            if (discountAmount == null
                    || discountAmount.signum() < 0) {

                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Invalid promotion discount"
                );
            }

            // A discount cannot exceed the subtotal.
            discountAmount = discountAmount.min(subtotal);
        }

        // 9. Calculate the final amount.
        BigDecimal totalAmount = subtotal.subtract(discountAmount);

        // 10. Associate the optional promotion with the reservation.
        reservation.setPromotion(promotion);

        reservation.setTotalAmount(totalAmount);

        // 11. Save the reservation and return its response.
        Reservation savedReservation =
                reservationRepository.save(reservation);

        return reservationMapper.toResponse(savedReservation);
    }
}