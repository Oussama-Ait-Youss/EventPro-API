package com.eventpro.api.service.impl;

import com.eventpro.api.dto.user.UserRegistrationRequest;
import com.eventpro.api.dto.user.UserResponse;
import com.eventpro.api.exception.EmailAlreadyExistsException;
import com.eventpro.api.mapper.UserMapper;
import com.eventpro.api.model.*;
import com.eventpro.api.repository.UserRepository;
import com.eventpro.api.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {


    private final UserRepository userRepository;
    private final UserMapper userMapper;



    @Override
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        // Map the request to the correct entity subtype.
        User user = userMapper.toNewUser(request);

        // Save the entity.
        User savedUser = userRepository.save(user);

        // Map to the response matching the entity subtype.
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }
}
