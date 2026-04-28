package com.queuecare.queuecare.controller;

import com.queuecare.queuecare.dto.UserResponse;
import com.queuecare.queuecare.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public List<UserResponse> getAllUsers(Authentication auth) {
        return userService.getAllUsers(getUserId(auth))
                .stream().map(UserResponse::from).toList();
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id, Authentication auth) {
        userService.deleteUser(getUserId(auth), id);
    }

    @GetMapping("/users/doctors")
    public List<UserResponse> getDoctors() {
        return userService.getDoctors()
                .stream().map(UserResponse::from).toList();
    }

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
