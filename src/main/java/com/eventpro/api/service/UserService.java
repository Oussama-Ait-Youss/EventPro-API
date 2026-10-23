package com.eventpro.api.service;

import com.eventpro.api.dto.user.UserRegistrationRequest;
import com.eventpro.api.dto.user.UserResponse;

public interface UserService {
    UserResponse register(UserRegistrationRequest request);
}
