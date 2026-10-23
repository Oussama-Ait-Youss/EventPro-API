package com.eventpro.api.mapper;

import com.eventpro.api.model.Admin;
import com.eventpro.api.model.Client;
import com.eventpro.api.model.AgentConrol;
import com.eventpro.api.model.Organizer;
import com.eventpro.api.model.User;
import com.eventpro.api.dto.user.UserRegistrationRequest;
import com.eventpro.api.dto.user.UserResponse;
import org.springframework.stereotype.Component;
import static com.eventpro.api.model.enums.UserType.*;

@Component
public class UserMapper {

    public User toNewUser(UserRegistrationRequest request) {
        User user = switch (request.getUserType()) {
            case ADMIN -> new Admin();
            case CLIENT -> new Client();
            case ORGANIZER -> new Organizer();
            case CONTROL_AGENT -> new AgentConrol();
        };
        updateUser(user, request);
        user.setActive(true);
        return user;
    }

    public void updateUser(User user, UserRegistrationRequest request) {
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole(request.getUserType());

        if (user instanceof Client client && request.getPhone() != null) {
            client.setPhone(request.getPhone());
        }
        if (user instanceof Organizer organizer && request.getOrganisationName() != null) {
            organizer.setOrganisationName(request.getOrganisationName());
        }
    }

    public void updateExistingUser(User user, UserRegistrationRequest request) {
        updateUser(user, request);
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(user);
    }
}