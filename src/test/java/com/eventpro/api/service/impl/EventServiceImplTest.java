package com.eventpro.api.service.impl;

import com.eventpro.api.dto.event.EventCreateRequest;
import com.eventpro.api.dto.event.EventResponse;
import com.eventpro.api.dto.event.EventUpdateRequest;
import com.eventpro.api.exception.EventNotFoundException;
import com.eventpro.api.mapper.EventMapper;
import com.eventpro.api.model.Event;
import com.eventpro.api.model.Organizer;
import com.eventpro.api.model.TicketCategoriesPerEvent;
import com.eventpro.api.model.enums.EventStatus;
import com.eventpro.api.repository.EventRepository;
import com.eventpro.api.repository.OrganizerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrganizerRepository organizerRepository;

    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void shouldCreateEventForOrganizer() {
        Organizer organizer = new Organizer();
        organizer.setId(7L);
        EventCreateRequest request = validCreateRequest();
        Event mappedEvent = new Event();
        EventResponse expected = new EventResponse();
        when(organizerRepository.findById(7L)).thenReturn(Optional.of(organizer));
        when(eventMapper.toEntity(request)).thenReturn(mappedEvent);
        when(eventRepository.save(mappedEvent)).thenReturn(mappedEvent);
        when(eventMapper.toResponse(mappedEvent)).thenReturn(expected);

        EventResponse result = eventService.createEvent(7L, request);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertSame(expected, result);
        assertSame(organizer, captor.getValue().getOrganizer());
        assertEquals(EventStatus.PUBLISHED, captor.getValue().getStatus());
    }

    @Test
    void shouldRejectCreateWhenEventDatesAreInvalid() {
        EventCreateRequest request = validCreateRequest();
        request.setEndDate(request.getStartDate());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.createEvent(7L, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(organizerRepository, eventMapper, eventRepository);
    }

    @Test
    void shouldReturnAllEventsAsResponses() {
        Event first = new Event();
        Event second = new Event();
        EventResponse firstResponse = new EventResponse();
        EventResponse secondResponse = new EventResponse();
        when(eventRepository.findAll()).thenReturn(List.of(first, second));
        when(eventMapper.toResponse(first)).thenReturn(firstResponse);
        when(eventMapper.toResponse(second)).thenReturn(secondResponse);

        List<EventResponse> result = eventService.getAllEvents();

        assertEquals(List.of(firstResponse, secondResponse), result);
    }

    @Test
    void shouldUpdateOwnedEventAndReturnResponse() {
        Organizer organizer = new Organizer();
        organizer.setId(7L);
        Event event = new Event();
        event.setOrganizer(organizer);
        event.setTicketCategories(new ArrayList<>());
        EventUpdateRequest request = validUpdateRequest();
        EventResponse expected = new EventResponse();
        when(eventRepository.findById(3L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(expected);

        assertSame(expected, eventService.updateEvent(7L, 3L, request));

        verify(eventMapper).updateEntityFromDto(request, event);
        verify(eventRepository).save(event);
    }

    @Test
    void shouldRejectUpdateWhenEventBelongsToAnotherOrganizer() {
        Organizer organizer = new Organizer();
        organizer.setId(8L);
        Event event = new Event();
        event.setOrganizer(organizer);
        when(eventRepository.findById(3L)).thenReturn(Optional.of(event));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.updateEvent(7L, 3L, validUpdateRequest())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdateBelowAssignedCategoryCapacity() {
        Organizer organizer = new Organizer();
        organizer.setId(7L);
        TicketCategoriesPerEvent category = new TicketCategoriesPerEvent();
        category.setTotalQuantity(60);
        Event event = new Event();
        event.setOrganizer(organizer);
        event.setTicketCategories(List.of(category));
        EventUpdateRequest request = validUpdateRequest();
        request.setCapacity(50);
        when(eventRepository.findById(3L)).thenReturn(Optional.of(event));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventService.updateEvent(7L, 3L, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingMissingEvent() {
        when(eventRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () -> eventService.updateEvent(7L, 3L, validUpdateRequest())
        );
    }

    private EventCreateRequest validCreateRequest() {
        LocalDateTime now = LocalDateTime.now();
        EventCreateRequest request = new EventCreateRequest();
        request.setStartDate(now.plusDays(4));
        request.setEndDate(now.plusDays(5));
        request.setSalesStartDate(now.plusDays(1));
        request.setSalesEndDate(now.plusDays(3));
        request.setStatus(EventStatus.PUBLISHED);
        return request;
    }

    private EventUpdateRequest validUpdateRequest() {
        LocalDateTime now = LocalDateTime.now();
        EventUpdateRequest request = new EventUpdateRequest();
        request.setStartDate(now.plusDays(4));
        request.setEndDate(now.plusDays(5));
        request.setSalesStartDate(now.plusDays(1));
        request.setSalesEndDate(now.plusDays(3));
        request.setCapacity(100);
        return request;
    }
}
