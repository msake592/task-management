package com.mahmutsalih.task_management.service.impl;

import com.mahmutsalih.task_management.dto.request.UserRequest;
import com.mahmutsalih.task_management.dto.response.UserResponse;
import com.mahmutsalih.task_management.entity.Role;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.exception.BadRequestException;
import com.mahmutsalih.task_management.exception.ResourceNotFoundException;
import com.mahmutsalih.task_management.repository.RoleRepository;
import com.mahmutsalih.task_management.repository.UserRepository;
import com.mahmutsalih.task_management.security.CurrentUserService;
import com.mahmutsalih.task_management.service.UserService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@example.com";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    @Override
    public Page<UserResponse> getAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    public UserResponse getById(Long id) {
        return toResponse(findUser(id));
    }

    @Override
    public User getEntityById(Long id) {
        return findUser(id);
    }

    @Override
    public UserResponse update(Long id, UserRequest request) {
        User user = findUser(id);
        Role newRole = findRole(request.getRoleId());
        boolean roleChanged = !Objects.equals(getRoleId(user.getRole()), getRoleId(newRole));

        if (roleChanged) {
            validateRoleChange(user, newRole);
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setRole(newRole);

        return toResponse(userRepository.save(user));
    }

    @Override
    public void delete(Long id) {
        User user = findUser(id);
        userRepository.delete(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private Role findRole(Long roleId) {
        if (roleId == null) {
            return null;
        }

        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));
    }

    private void validateRoleChange(User user, Role newRole) {
        User currentUser = currentUserService.getCurrentUser();
        if (Objects.equals(currentUser.getId(), user.getId())) {
            throw new AccessDeniedException("You cannot change your own role");
        }

        if (DEFAULT_ADMIN_EMAIL.equalsIgnoreCase(user.getEmail())) {
            throw new AccessDeniedException("This admin user's role cannot be changed");
        }

        if (isAdminRole(user.getRole()) && !isAdminRole(newRole)
                && !userRepository.existsByRole_NameAndIdNot(ADMIN_ROLE, user.getId())) {
            throw new BadRequestException("At least one admin must remain in the system");
        }
    }

    private Long getRoleId(Role role) {
        return role != null ? role.getId() : null;
    }

    private boolean isAdminRole(Role role) {
        return role != null && ADMIN_ROLE.equals(role.getName());
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
