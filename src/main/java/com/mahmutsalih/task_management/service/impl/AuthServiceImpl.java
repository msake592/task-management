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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

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
                .role(findRole(request.getRoleId()))
                .build();

        return toResponse(userRepository.save(user));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .build();
    }

    private Role findRole(Long roleId) {
        if (roleId == null) {
            return null;
        }

        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));
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
