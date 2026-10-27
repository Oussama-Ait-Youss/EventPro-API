package com.eventpro.api.service;

import com.eventpro.api.dto.user.UserRegistrationRequest;
import com.eventpro.api.dto.user.UserResponse;
import com.eventpro.api.dto.user.UserUpdateRequest;

import java.util.List;

public interface UserService {
    UserResponse register(UserRegistrationRequest request);
    List<UserResponse> getAllUsers();
    UserResponse updateUser(Long userId, UserUpdateRequest request);
}
