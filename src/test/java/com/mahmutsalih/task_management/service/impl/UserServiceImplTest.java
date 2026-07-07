package com.mahmutsalih.task_management.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mahmutsalih.task_management.dto.request.UserRequest;
import com.mahmutsalih.task_management.dto.response.UserResponse;
import com.mahmutsalih.task_management.entity.Role;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.exception.BadRequestException;
import com.mahmutsalih.task_management.exception.ResourceNotFoundException;
import com.mahmutsalih.task_management.repository.RoleRepository;
import com.mahmutsalih.task_management.repository.UserRepository;
import com.mahmutsalih.task_management.security.CurrentUserService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void update_shouldUpdateUserAndRole() {
        Role adminRole = Role.builder().id(2L).name("ADMIN").build();
        User user = User.builder()
                .id(10L)
                .firstName("Mahmut")
                .lastName("Kelkit")
                .email("mahmut@example.com")
                .password("old-password")
                .role(Role.builder().id(1L).name("USER").build())
                .build();
        User currentAdmin = User.builder().id(99L).email("current-admin@example.com").build();
        UserRequest request = UserRequest.builder()
                .firstName("Mahmut")
                .lastName("Admin")
                .email("admin@example.com")
                .password("new-password")
                .roleId(2L)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));
        when(currentUserService.getCurrentUser()).thenReturn(currentAdmin);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        UserResponse response = userService.update(10L, request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("admin@example.com");
        assertThat(response.getRoleId()).isEqualTo(2L);
        assertThat(response.getRoleName()).isEqualTo("ADMIN");
        verify(passwordEncoder).encode("new-password");
    }

    @Test
    void update_whenRoleDoesNotChange_shouldSkipRoleSecurityChecks() {
        Role userRole = Role.builder().id(1L).name("USER").build();
        User user = User.builder()
                .id(10L)
                .firstName("Mahmut")
                .lastName("Kelkit")
                .email("mahmut@example.com")
                .password("old-password")
                .role(userRole)
                .build();
        UserRequest request = UserRequest.builder()
                .firstName("Mahmut")
                .lastName("Kelkit")
                .email("mahmut@example.com")
                .roleId(1L)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.update(10L, request);

        assertThat(response.getRoleName()).isEqualTo("USER");
        assertThat(user.getPassword()).isEqualTo("old-password");
        verifyNoInteractions(currentUserService);
    }

    @Test
    void update_whenAdminChangesOwnRole_shouldThrowAccessDeniedException() {
        Role userRole = Role.builder().id(1L).name("USER").build();
        User admin = User.builder()
                .id(10L)
                .email("admin@example.com")
                .password("encoded-password")
                .role(Role.builder().id(2L).name("ADMIN").build())
                .build();
        UserRequest request = UserRequest.builder()
                .firstName("Admin")
                .email("admin@example.com")
                .roleId(1L)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(admin));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(userRole));
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        assertThatThrownBy(() -> userService.update(10L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You cannot change your own role");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void update_whenOnlyAdminWouldBeDemoted_shouldThrowBadRequestException() {
        Role userRole = Role.builder().id(1L).name("USER").build();
        User admin = User.builder()
                .id(10L)
                .email("other-admin@example.com")
                .password("encoded-password")
                .role(Role.builder().id(2L).name("ADMIN").build())
                .build();
        User currentAdmin = User.builder().id(99L).email("current-admin@example.com").build();
        UserRequest request = UserRequest.builder()
                .firstName("Admin")
                .email("other-admin@example.com")
                .roleId(1L)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(admin));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(userRole));
        when(currentUserService.getCurrentUser()).thenReturn(currentAdmin);
        when(userRepository.existsByRole_NameAndIdNot("ADMIN", 10L)).thenReturn(false);

        assertThatThrownBy(() -> userService.update(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("At least one admin must remain in the system");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void update_whenAnotherAdminExists_shouldAllowAdminDemotion() {
        Role userRole = Role.builder().id(1L).name("USER").build();
        User admin = User.builder()
                .id(10L)
                .firstName("Other")
                .email("other-admin@example.com")
                .password("encoded-password")
                .role(Role.builder().id(2L).name("ADMIN").build())
                .build();
        User currentAdmin = User.builder().id(99L).email("current-admin@example.com").build();
        UserRequest request = UserRequest.builder()
                .firstName("Other")
                .email("other-admin@example.com")
                .roleId(1L)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(admin));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(userRole));
        when(currentUserService.getCurrentUser()).thenReturn(currentAdmin);
        when(userRepository.existsByRole_NameAndIdNot("ADMIN", 10L)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.update(10L, request);

        assertThat(response.getRoleName()).isEqualTo("USER");
    }

    @Test
    void update_whenDefaultAdminRoleChanges_shouldThrowAccessDeniedException() {
        Role userRole = Role.builder().id(1L).name("USER").build();
        User defaultAdmin = User.builder()
                .id(10L)
                .email("admin@example.com")
                .password("encoded-password")
                .role(Role.builder().id(2L).name("ADMIN").build())
                .build();
        User currentAdmin = User.builder().id(99L).email("current-admin@example.com").build();
        UserRequest request = UserRequest.builder()
                .firstName("Admin")
                .email("admin@example.com")
                .roleId(1L)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(defaultAdmin));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(userRole));
        when(currentUserService.getCurrentUser()).thenReturn(currentAdmin);

        assertThatThrownBy(() -> userService.update(10L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("This admin user's role cannot be changed");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void getById_shouldReturnUser() {
        Role role = Role.builder().id(1L).name("USER").build();
        User user = User.builder()
                .id(10L)
                .firstName("Mahmut")
                .lastName("Kelkit")
                .email("mahmut@example.com")
                .password("encoded-password")
                .role(role)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getFirstName()).isEqualTo("Mahmut");
        assertThat(response.getRoleName()).isEqualTo("USER");
    }

    @Test
    void getById_whenUserNotFound_shouldThrowResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 99");
    }
}
