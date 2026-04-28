package com.queuecare.queuecare.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.queuecare.queuecare.dto.AuthResponse;
import com.queuecare.queuecare.dto.LoginRequest;
import com.queuecare.queuecare.dto.RegisterRequest;
import com.queuecare.queuecare.dto.UserResponse;
import com.queuecare.queuecare.model.User;
import com.queuecare.queuecare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(userService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request);
        return new AuthResponse(token);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return UserResponse.from(userService.getById(userId));
    }
}
