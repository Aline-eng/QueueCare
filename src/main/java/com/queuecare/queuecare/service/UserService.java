package com.queuecare.queuecare.service;

import org.springframework.stereotype.Service;

import com.queuecare.queuecare.dto.LoginRequest;
import com.queuecare.queuecare.dto.RegisterRequest;
import com.queuecare.queuecare.exception.ConflictException;
import com.queuecare.queuecare.exception.UnauthorizedException;
import com.queuecare.queuecare.model.User;
import com.queuecare.queuecare.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // REGISTER
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .role(request.getRole() == null ? User.Role.PATIENT : request.getRole())
                .build();
        return userRepository.save(user);
    }

    //LOGIN
    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return TokenService.generateToken(user.getId());
    }
}
