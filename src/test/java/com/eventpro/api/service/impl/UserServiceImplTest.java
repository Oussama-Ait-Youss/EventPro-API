package com.eventpro.api.service.impl;

import com.eventpro.api.dto.user.UserRegistrationRequest;
import com.eventpro.api.dto.user.UserResponse;
import com.eventpro.api.dto.user.UserUpdateRequest;
import com.eventpro.api.exception.EmailAlreadyExistsException;
import com.eventpro.api.mapper.UserMapper;
import com.eventpro.api.model.Client;
import com.eventpro.api.model.User;
import com.eventpro.api.model.enums.UserType;
import com.eventpro.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldRegisterUserWhenEmailIsAvailable() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("client@example.com");
        request.setUserType(UserType.CLIENT);
        Client client = new Client();
        UserResponse expected = new UserResponse();
        when(userRepository.existsByEmail("client@example.com")).thenReturn(false);
        when(userMapper.toNewUser(request)).thenReturn(client);
        when(userRepository.save(client)).thenReturn(client);
        when(userMapper.toResponse(client)).thenReturn(expected);

        assertSame(expected, userService.register(request));

        verify(userRepository).save(client);
    }

    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("client@example.com");
        when(userRepository.existsByEmail("client@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.register(request));

        verifyNoInteractions(userMapper);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldReturnOnlyActiveUsers() {
        Client active = new Client();
        active.setActive(true);
        Client inactive = new Client();
        inactive.setActive(false);
        UserResponse response = new UserResponse();
        when(userRepository.findAll()).thenReturn(List.of(active, inactive));
        when(userMapper.toResponse(active)).thenReturn(response);

        assertEquals(List.of(response), userService.getAllUsers());
        verify(userMapper, never()).toResponse(inactive);
    }

    @Test
    void shouldDeactivateAndRestoreUser() {
        Client user = new Client();
        user.setActive(true);
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(new UserResponse());

        userService.deleteUser(4L);
        assertFalse(user.isActive());

        userService.restoreUser(4L);
        assertTrue(user.isActive());
    }

    @Test
    void shouldUpdateUserAndSaveChangedDetails() {
        Client user = new Client();
        user.setEmail("old@example.com");
        UserUpdateRequest request = new UserUpdateRequest();
        request.setNom("Updated Name");
        request.setEmail("new@example.com");
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(new UserResponse());

        userService.updateUser(4L, request);

        assertEquals("Updated Name", user.getName());
        assertEquals("new@example.com", user.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectUpdateWhenEmailBelongsToAnotherUser() {
        Client user = new Client();
        user.setEmail("old@example.com");
        UserUpdateRequest request = new UserUpdateRequest();
        request.setNom("Updated Name");
        request.setEmail("taken@example.com");
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.updateUser(4L, request)
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectUpdatingMissingUser() {
        when(userRepository.findById(4L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(4L, new UserUpdateRequest())
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
