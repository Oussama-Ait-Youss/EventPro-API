package com.eventpro.api.service.impl;

import com.eventpro.api.dto.user.UserRegistrationRequest;
import com.eventpro.api.dto.user.UserResponse;
import com.eventpro.api.exception.EmailAlreadyExistsException;
import com.eventpro.api.exception.InvalidUserTypeException;
import com.eventpro.api.mapper.UserMapper;
import com.eventpro.api.model.*;
import com.eventpro.api.repository.UserRepository;
import com.eventpro.api.service.UserService;
import jakarta.transaction.Transactional;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {


    private final UserRepository userRepository;
    private final UserMapper userMapper;



    @Override
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {

        // 1. Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        // 2. Create the correct subclass
        User user;

        switch (request.getUserType()) {

            case ADMIN -> {
                user = new Admin();
            }

            case ORGANIZER -> {
                Organizer organizer = new Organizer();
                organizer.setOrganisationName(request.getOrganisationName());
                user = organizer;
            }

            case CLIENT -> {
                Client client = new Client();
                client.setPhone(request.getPhone());
                user = client;
            }

            case CONTROL_AGENT -> {
                user = new AgentConrol();
            }

            default -> throw new InvalidUserTypeException("Invalid user type");
        }

        // 3. Set common attributes
        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // 4. Save the user
        User savedUser = userRepository.save(user);

        // 5. Convert Entity to DTO
        return userMapper.toResponse(savedUser);
    }
}
