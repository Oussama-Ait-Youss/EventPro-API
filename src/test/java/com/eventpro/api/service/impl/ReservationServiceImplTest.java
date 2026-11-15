package com.eventpro.api.service.impl;

import com.eventpro.api.dto.reservation.ReservationCreateRequest;
import com.eventpro.api.dto.reservation.ReservationLineRequest;
import com.eventpro.api.dto.reservation.ReservationResponse;
import com.eventpro.api.mapper.ReservationMapper;
import com.eventpro.api.model.Client;
import com.eventpro.api.model.Event;
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
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationLineRepository reservationLineRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketCategoriesPerEventRepository categoryRepository;

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private ReservationMapper reservationMapper;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @Test
    void shouldCreateReservationAndReserveInventory() {
        Client client = new Client();
        client.setId(3L);
        Event event = openEvent();
        TicketCategoriesPerEvent category = availableCategory(event);
        ReservationCreateRequest request = requestFor(5L, 2);
        ReservationResponse expected = new ReservationResponse();
        when(clientRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(client));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        when(categoryRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(category));
        when(reservationLineRepository.countActiveTicketsByClientAndCategory(
                eq(3L), eq(8L), eq(ReservationStatus.CONFIRMED),
                eq(ReservationStatus.PENDING), any(LocalDateTime.class)
        )).thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationMapper.toResponse(any(Reservation.class))).thenReturn(expected);

        ReservationResponse result = reservationService.createReservation(3L, request);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        Reservation saved = captor.getValue();
        assertSame(expected, result);
        assertEquals(ReservationStatus.PENDING, saved.getStatus());
        assertEquals(0, saved.getTotalAmount().compareTo(BigDecimal.valueOf(25)));
        assertEquals(2, category.getReservedQuantity());
        assertEquals(1, saved.getLines().size());
        assertTrue(saved.getExpirationDate().isAfter(saved.getCreationDate()));
    }

    @Test
    void shouldRejectInvalidReservationLineBeforeRepositoryCalls() {
        ReservationCreateRequest request = requestFor(5L, 0);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reservationService.createReservation(3L, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(clientRepository, eventRepository, categoryRepository);
    }

    @Test
    void shouldRejectReservationsForUnpublishedEvent() {
        Client client = new Client();
        Event event = openEvent();
        event.setStatus(EventStatus.DRAFT);
        when(clientRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(client));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reservationService.createReservation(3L, requestFor(5L, 1))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(categoryRepository);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldRejectReservationWhenTicketStockIsInsufficient() {
        Client client = new Client();
        Event event = openEvent();
        TicketCategoriesPerEvent category = availableCategory(event);
        category.setTotalQuantity(1);
        when(clientRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(client));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        when(categoryRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(category));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reservationService.createReservation(3L, requestFor(5L, 2))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(reservationRepository, never()).save(any());
        assertEquals(0, category.getReservedQuantity());
    }

    @Test
    void shouldReturnReservationForOwningClient() {
        Client client = new Client();
        client.setId(3L);
        Reservation reservation = new Reservation();
        reservation.setClient(client);
        ReservationResponse expected = new ReservationResponse();
        when(reservationRepository.findById(9L)).thenReturn(Optional.of(reservation));
        when(reservationMapper.toResponse(reservation)).thenReturn(expected);

        assertSame(expected, reservationService.getReservation(3L, 9L));
    }

    @Test
    void shouldRejectReadingAnotherClientsReservation() {
        Client client = new Client();
        client.setId(4L);
        Reservation reservation = new Reservation();
        reservation.setClient(client);
        when(reservationRepository.findById(9L)).thenReturn(Optional.of(reservation));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reservationService.getReservation(3L, 9L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void shouldCancelPendingReservationAndReleaseStock() {
        Client client = new Client();
        client.setId(3L);
        TicketCategoriesPerEvent lineCategory = new TicketCategoriesPerEvent();
        lineCategory.setId(8L);
        TicketCategoriesPerEvent lockedCategory = new TicketCategoriesPerEvent();
        lockedCategory.setReservedQuantity(3);
        ReservationLine line = new ReservationLine();
        line.setQuantity(2);
        line.setTicketCategoryPerEvent(lineCategory);
        Reservation reservation = pendingReservation(client, line);
        when(reservationRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(reservation));
        when(categoryRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(lockedCategory));
        when(reservationRepository.save(reservation)).thenReturn(reservation);
        when(reservationMapper.toResponse(reservation)).thenReturn(new ReservationResponse());

        ReservationResponse response = reservationService.cancelReservation(3L, 9L);

        assertNotNull(response);
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(1, lockedCategory.getReservedQuantity());
        verify(reservationRepository).save(reservation);
    }

    @Test
    void shouldExpirePendingReservationOnceExpirationHasPassed() {
        Client client = new Client();
        client.setId(3L);
        TicketCategoriesPerEvent lineCategory = new TicketCategoriesPerEvent();
        lineCategory.setId(8L);
        TicketCategoriesPerEvent lockedCategory = new TicketCategoriesPerEvent();
        lockedCategory.setReservedQuantity(1);
        ReservationLine line = new ReservationLine();
        line.setQuantity(1);
        line.setTicketCategoryPerEvent(lineCategory);
        Reservation reservation = pendingReservation(client, line);
        reservation.setExpirationDate(LocalDateTime.now().minusMinutes(1));
        when(reservationRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(reservation));
        when(categoryRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(lockedCategory));

        reservationService.expireReservation(9L);

        assertEquals(ReservationStatus.EXPIRED, reservation.getStatus());
        assertEquals(0, lockedCategory.getReservedQuantity());
        verify(reservationRepository).save(reservation);
    }

    @Test
    void shouldNotChangeReservationThatHasNotExpired() {
        Client client = new Client();
        Reservation reservation = pendingReservation(client);
        reservation.setExpirationDate(LocalDateTime.now().plusMinutes(1));
        when(reservationRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(reservation));

        reservationService.expireReservation(9L);

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        verifyNoInteractions(categoryRepository);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldSearchReservationsForClientEmailAfterValidatingEvent() {
        Event event = new Event();
        Client client = new Client();
        client.setId(3L);
        Reservation reservation = new Reservation();
        ReservationResponse expected = new ReservationResponse();
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        when(clientRepository.existsByEmail("person@example.com")).thenReturn(true);
        when(clientRepository.findByEmail("person@example.com")).thenReturn(Optional.of(client));
        when(reservationRepository.findByClient_Id(3L)).thenReturn(List.of(reservation));
        when(reservationMapper.toResponse(reservation)).thenReturn(expected);

        List<ReservationResponse> result = reservationService.RechercheReservation(
                5L, "PENDING", "person@example.com", "General", 1
        );

        assertEquals(List.of(expected), result);
    }

    @Test
    void shouldListClientReservationsForExistingClient() {
        when(clientRepository.existsById(3L)).thenReturn(true);
        when(reservationRepository.findByClient_Id(3L)).thenReturn(List.of());

        assertTrue(reservationService.getClientReservations(3L).isEmpty());
    }

    private ReservationCreateRequest requestFor(Long eventId, int quantity) {
        ReservationLineRequest line = new ReservationLineRequest();
        line.setEventTicketCategoryId(8L);
        line.setQuantity(quantity);
        ReservationCreateRequest request = new ReservationCreateRequest();
        request.setEventId(eventId);
        request.setLines(List.of(line));
        return request;
    }

    private Event openEvent() {
        Event event = new Event();
        event.setId(5L);
        event.setStatus(EventStatus.PUBLISHED);
        event.setSalesOpeningDate(LocalDateTime.now().minusDays(1));
        event.setSalesClosingDate(LocalDateTime.now().plusDays(1));
        return event;
    }

    private TicketCategoriesPerEvent availableCategory(Event event) {
        TicketCategoriesPerEvent category = new TicketCategoriesPerEvent();
        category.setId(8L);
        category.setEvent(event);
        category.setUnitPrice(BigDecimal.valueOf(12.5));
        category.setTotalQuantity(10);
        category.setReservedQuantity(0);
        category.setSoldQuantity(0);
        category.setMaxQuotaPerClient(5);
        return category;
    }

    private Reservation pendingReservation(Client client, ReservationLine... lines) {
        Reservation reservation = new Reservation();
        reservation.setId(9L);
        reservation.setClient(client);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setExpirationDate(LocalDateTime.now().minusMinutes(1));
        for (ReservationLine line : lines) {
            reservation.addLine(line);
        }
        return reservation;
    }
}
