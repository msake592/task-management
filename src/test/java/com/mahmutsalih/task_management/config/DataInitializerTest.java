package com.mahmutsalih.task_management.config;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahmutsalih.task_management.entity.Role;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.repository.RoleRepository;
import com.mahmutsalih.task_management.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void run_shouldSeedUserAndAdminRolesWhenMissing() {
        DataInitializer dataInitializer = new DataInitializer(roleRepository, userRepository, passwordEncoder);
        Role adminRole = Role.builder().id(1L).name("ADMIN").build();
        when(roleRepository.existsByName("USER")).thenReturn(false);
        when(roleRepository.existsByName("ADMIN")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("admin123")).thenReturn("encoded-admin-password");

        dataInitializer.run();

        verify(roleRepository).save(argThat(role -> "USER".equals(role.getName())));
        verify(roleRepository).save(argThat(role -> "ADMIN".equals(role.getName())));
        verify(userRepository).save(argThat(user ->
                "Admin".equals(user.getFirstName())
                        && "User".equals(user.getLastName())
                        && "admin@example.com".equals(user.getEmail())
                        && "encoded-admin-password".equals(user.getPassword())
                        && adminRole.equals(user.getRole())
        ));
    }

    @Test
    void run_whenRolesExist_shouldNotCreateDuplicates() {
        DataInitializer dataInitializer = new DataInitializer(roleRepository, userRepository, passwordEncoder);
        when(roleRepository.existsByName("USER")).thenReturn(true);
        when(roleRepository.existsByName("ADMIN")).thenReturn(true);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

        dataInitializer.run();

        verify(roleRepository, never()).save(org.mockito.ArgumentMatchers.any(Role.class));
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }
}
