package com.eventpro.api.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {

    @NotBlank(message = "Name is required")
    private String nom;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}