package com.queuecare.queuecare.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.queuecare.queuecare.dto.LoginRequest;
import com.queuecare.queuecare.dto.RegisterRequest;
import com.queuecare.queuecare.exception.ConflictException;
import com.queuecare.queuecare.exception.UnauthorizedException;
import com.queuecare.queuecare.exception.ForbiddenException;
import com.queuecare.queuecare.exception.NotFoundException;
import com.queuecare.queuecare.model.User;
import com.queuecare.queuecare.repository.UserRepository;
import java.util.List;

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

    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public List<User> getAllUsers(Long requesterId) {
        User requester = getById(requesterId);
        if (requester.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Only admins can view all users");
        }
        return userRepository.findAll();
    }

    public void deleteUser(Long requesterId, Long targetId) {
        User requester = getById(requesterId);
        if (requester.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Only admins can delete users");
        }
        if (requesterId.equals(targetId)) {
            throw new ConflictException("You cannot delete your own account");
        }
        userRepository.deleteById(targetId);
    }

    public List<User> getDoctors() {
        return userRepository.findByRole(User.Role.STAFF);
    }
}