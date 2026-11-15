package com.eventpro.api.service.impl;

import com.eventpro.api.dto.event.EventTicketCategoryAssignRequest;
import com.eventpro.api.dto.event.EventTicketCategoryResponse;
import com.eventpro.api.model.Event;
import com.eventpro.api.model.TicketCategoriesPerEvent;
import com.eventpro.api.model.TicketCategory;
import com.eventpro.api.repository.EventRepository;
import com.eventpro.api.repository.TicketCategoriesPerEventRepository;
import com.eventpro.api.repository.TicketCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventTicketCategoryServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketCategoryRepository ticketCategoryRepository;

    @Mock
    private TicketCategoriesPerEventRepository eventCategoryRepository;

    @InjectMocks
    private EventTicketCategoryServiceImpl service;

    @Test
    void shouldAssignCategoryAndInitializeInventory() {
        Event event = new Event();
        event.setCapacity(40);
        event.setTicketCategories(new ArrayList<>());
        TicketCategory ticketCategory = new TicketCategory();
        ticketCategory.setId(5L);
        ticketCategory.setName("General");
        EventTicketCategoryAssignRequest request = validRequest();
        when(eventRepository.findById(2L)).thenReturn(Optional.of(event));
        when(ticketCategoryRepository.findById(5L)).thenReturn(Optional.of(ticketCategory));
        when(eventCategoryRepository.existsByEvent_IdAndTicketCategory_Id(2L, 5L)).thenReturn(false);
        when(eventCategoryRepository.save(any(TicketCategoriesPerEvent.class)))
                .thenAnswer(invocation -> {
                    TicketCategoriesPerEvent category = invocation.getArgument(0);
                    category.setId(9L);
                    return category;
                });

        EventTicketCategoryResponse response = service.assignCategory(2L, request);

        ArgumentCaptor<TicketCategoriesPerEvent> captor =
                ArgumentCaptor.forClass(TicketCategoriesPerEvent.class);
        verify(eventCategoryRepository).save(captor.capture());
        assertEquals(9L, response.getId());
        assertEquals("General", response.getName());
        assertEquals(BigDecimal.valueOf(25), response.getPrice());
        assertEquals(0, response.getReservedQuantity());
        assertEquals(0, response.getSoldQuantity());
        assertSame(event, captor.getValue().getEvent());
    }

    @Test
    void shouldRejectDuplicateCategoryAssignment() {
        Event event = new Event();
        event.setTicketCategories(new ArrayList<>());
        TicketCategory category = new TicketCategory();
        category.setId(5L);
        when(eventRepository.findById(2L)).thenReturn(Optional.of(event));
        when(ticketCategoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(eventCategoryRepository.existsByEvent_IdAndTicketCategory_Id(2L, 5L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.assignCategory(2L, validRequest())
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(eventCategoryRepository, never()).save(any());
    }

    @Test
    void shouldRejectQuantityThatExceedsEventCapacity() {
        Event event = new Event();
        event.setCapacity(10);
        event.setTicketCategories(new ArrayList<>());
        TicketCategory category = new TicketCategory();
        category.setId(5L);
        when(eventRepository.findById(2L)).thenReturn(Optional.of(event));
        when(ticketCategoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(eventCategoryRepository.existsByEvent_IdAndTicketCategory_Id(2L, 5L)).thenReturn(false);
        EventTicketCategoryAssignRequest request = validRequest();
        request.setQuantity(11);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.assignCategory(2L, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(eventCategoryRepository, never()).save(any());
    }

    private EventTicketCategoryAssignRequest validRequest() {
        EventTicketCategoryAssignRequest request = new EventTicketCategoryAssignRequest();
        request.setTicketCategoryId(5L);
        request.setPrice(BigDecimal.valueOf(25));
        request.setQuantity(20);
        request.setMaxQuotaPerClient(4);
        return request;
    }
}
