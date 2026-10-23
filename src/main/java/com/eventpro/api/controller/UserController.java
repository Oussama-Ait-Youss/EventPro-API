package com.eventpro.api.controller;


import com.eventpro.api.dto.user.UserRegistrationRequest;
import com.eventpro.api.dto.user.UserResponse;
import com.eventpro.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {



    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody UserRegistrationRequest request) {

        return userService.register(request);

    }

}
