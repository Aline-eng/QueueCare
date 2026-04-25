package com.queuecare.queuecare.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.queuecare.queuecare.dto.AuthResponse;
import com.queuecare.queuecare.dto.LoginRequest;
import com.queuecare.queuecare.dto.RegisterRequest;
import com.queuecare.queuecare.model.User;
import com.queuecare.queuecare.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping("/register")
    public User register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request);
        return new AuthResponse(token);
    }
}
