package com.eventpro.api.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizerResponse extends UserResponse {

    private String organisationName;
}