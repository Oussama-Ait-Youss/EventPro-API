package com.eventpro.api.service;

import com.eventpro.api.dto.user.UserRegistrationRequest;
import com.eventpro.api.dto.user.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse register(UserRegistrationRequest request);
    List<UserResponse> getAllUsers();
}
