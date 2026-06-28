package com.mahmutsalih.task_management.service.impl;

import com.mahmutsalih.task_management.dto.request.LoginRequest;
import com.mahmutsalih.task_management.dto.request.RegisterRequest;
import com.mahmutsalih.task_management.dto.response.AuthResponse;
import com.mahmutsalih.task_management.dto.response.UserResponse;
import com.mahmutsalih.task_management.entity.Role;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.exception.BadRequestException;
import com.mahmutsalih.task_management.exception.ResourceNotFoundException;
import com.mahmutsalih.task_management.repository.RoleRepository;
import com.mahmutsalih.task_management.repository.UserRepository;
import com.mahmutsalih.task_management.security.JwtService;
import com.mahmutsalih.task_management.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String DEFAULT_USER_ROLE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(findDefaultUserRole())
                .build();

        User savedUser = userRepository.save(user);
        logger.info("User registered successfully. userId={}, username={}", savedUser.getId(), savedUser.getEmail());
        return toResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        logger.info("Login attempt. email={}", request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    logger.warn("Login failed. email={}", request.getEmail());
                    return new BadRequestException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Login failed. email={}", request.getEmail());
            throw new BadRequestException("Invalid email or password");
        }

        logger.info("Login successful. email={}", request.getEmail());
        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .build();
    }

    private Role findDefaultUserRole() {
        return roleRepository.findByName(DEFAULT_USER_ROLE)
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found: " + DEFAULT_USER_ROLE));
    }

    private UserResponse toResponse(User user) {
        Role role = user.getRole();

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .roleId(role != null ? role.getId() : null)
                .roleName(role != null ? role.getName() : null)
                .build();
    }
}
