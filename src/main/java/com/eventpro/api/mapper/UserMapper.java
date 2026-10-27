package com.eventpro.api.mapper;

import com.eventpro.api.dto.user.*;
import com.eventpro.api.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // =========================================
    // REQUEST -> ENTITY
    // =========================================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "role", source = "userType")
    @Mapping(target = "reservationList", ignore = true)
    Client toClient(UserRegistrationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "role", source = "userType")
    @Mapping(target = "eventList", ignore = true)
    Organizer toOrganizer(UserRegistrationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "role", source = "userType")
    Admin toAdmin(UserRegistrationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "role", source = "userType")

    AgentConrol toAgentControl(UserRegistrationRequest request);


    // =========================================
    // UPDATE EXISTING USER
    // =========================================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "role", source = "userType")
    void updateUser(
            UserRegistrationRequest request,
            @MappingTarget User user
    );


    // =========================================
    // ENTITY -> ROLE-SPECIFIC RESPONSE
    // =========================================

    @Mapping(target = "userType", source = "role")
    AdminResponse toAdminResponse(Admin user);

    @Mapping(target = "userType", source = "role")
    OrganizerResponse toOrganizerResponse(Organizer user);

    @Mapping(target = "userType", source = "role")
    ClientResponse toClientResponse(Client user);

    @Mapping(target = "userType", source = "role")
    ControlAgentResponse toControlAgentResponse(AgentConrol user);


    // =========================================
    // SELECT RESPONSE BASED ON USER SUBTYPE
    // =========================================

    default UserResponse toResponse(User user) {

        if (user == null) {
            return null;
        }

        if (user instanceof Admin admin) {
            return toAdminResponse(admin);
        }

        if (user instanceof Organizer organizer) {
            return toOrganizerResponse(organizer);
        }

        if (user instanceof Client client) {
            return toClientResponse(client);
        }

        if (user instanceof AgentConrol agent) {
            return toControlAgentResponse(agent);
        }

        throw new IllegalArgumentException(
                "Unsupported user type: " + user.getClass().getName()
        );
    }


    // =========================================
    // SELECT ENTITY BASED ON REQUESTED ROLE
    // =========================================

    default User toNewUser(UserRegistrationRequest request) {

        if (request == null || request.getUserType() == null) {
            throw new IllegalArgumentException("User type is required");
        }

        return switch (request.getUserType()) {
            case ADMIN -> toAdmin(request);
            case CLIENT -> toClient(request);
            case ORGANIZER -> toOrganizer(request);
            case CONTROL_AGENT -> toAgentControl(request);
        };
    }
}
