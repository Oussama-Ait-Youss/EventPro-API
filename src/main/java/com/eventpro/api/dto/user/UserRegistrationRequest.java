package com.eventpro.api.dto.user;

import com.eventpro.api.model.enums.UserType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class UserRegistrationRequest {



    @NotBlank (message = "name is required")
    private String name;

    @NotBlank (message = "email is required")
    @Email (message = "invalide email format")
    private String email;

    @NotBlank (message = "user type is required")
    private UserType userType;


    private String organization_name;
    private String telephone;

}
