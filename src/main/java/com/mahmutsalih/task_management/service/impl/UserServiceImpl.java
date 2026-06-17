package com.mahmutsalih.task_management.service.impl;

import com.mahmutsalih.task_management.dto.request.UserRequest;
import com.mahmutsalih.task_management.dto.response.UserResponse;
import com.mahmutsalih.task_management.entity.Role;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.exception.ResourceNotFoundException;
import com.mahmutsalih.task_management.repository.RoleRepository;
import com.mahmutsalih.task_management.repository.UserRepository;
import com.mahmutsalih.task_management.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public UserResponse create(UserRequest request) {
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(request.getPassword())
                .role(findRole(request.getRoleId()))
                .build();

        return toResponse(userRepository.save(user));
    }

    @Override
    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public UserResponse getById(Long id) {
        return toResponse(findUser(id));
    }

    @Override
    public UserResponse update(Long id, UserRequest request) {
        User user = findUser(id);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole(findRole(request.getRoleId()));

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
