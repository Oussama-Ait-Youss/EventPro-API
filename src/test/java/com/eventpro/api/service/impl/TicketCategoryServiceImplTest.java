package com.eventpro.api.service.impl;

import com.eventpro.api.dto.ticketCategory.TicketCategoryCreateRequest;
import com.eventpro.api.dto.ticketCategory.TicketCategoryResponse;
import com.eventpro.api.model.TicketCategory;
import com.eventpro.api.repository.TicketCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketCategoryServiceImplTest {

    @Mock
    private TicketCategoryRepository repository;

    @InjectMocks
    private TicketCategoryServiceImpl service;

    @Test
    void shouldTrimAndCreateCategory() {
        TicketCategoryCreateRequest request = new TicketCategoryCreateRequest();
        request.setName("  Premium  ");
        TicketCategory saved = new TicketCategory();
        saved.setId(4L);
        saved.setName("Premium");
        when(repository.findByNameIgnoreCase("Premium")).thenReturn(Optional.empty());
        when(repository.save(any(TicketCategory.class))).thenReturn(saved);

        TicketCategoryResponse response = service.createCategory(request);

        ArgumentCaptor<TicketCategory> captor = ArgumentCaptor.forClass(TicketCategory.class);
        verify(repository).save(captor.capture());
        assertEquals("Premium", captor.getValue().getName());
        assertEquals(4L, response.getId());
        assertEquals("Premium", response.getName());
    }

    @Test
    void shouldRejectExistingCategoryWithoutSaving() {
        TicketCategoryCreateRequest request = new TicketCategoryCreateRequest();
        request.setName("Premium");
        when(repository.findByNameIgnoreCase("Premium"))
                .thenReturn(Optional.of(new TicketCategory()));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createCategory(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(repository, never()).save(any());
    }
}
