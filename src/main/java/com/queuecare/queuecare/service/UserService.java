package com.queuecare.queuecare.service;

import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // REGISTER
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() == null ? User.Role.PATIENT : request.getRole())
                .build();
        return userRepository.save(user);
    }

    //LOGIN
    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return TokenService.generateToken(user.getId());
    }
}
