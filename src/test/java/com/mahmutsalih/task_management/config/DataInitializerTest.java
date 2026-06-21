package com.mahmutsalih.task_management.config;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahmutsalih.task_management.entity.Role;
import com.mahmutsalih.task_management.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private RoleRepository roleRepository;

    @Test
    void run_shouldSeedUserAndAdminRolesWhenMissing() {
        DataInitializer dataInitializer = new DataInitializer(roleRepository);
        when(roleRepository.existsByName("USER")).thenReturn(false);
        when(roleRepository.existsByName("ADMIN")).thenReturn(false);

        dataInitializer.run();

        verify(roleRepository).save(argThat(role -> "USER".equals(role.getName())));
        verify(roleRepository).save(argThat(role -> "ADMIN".equals(role.getName())));
    }

    @Test
    void run_whenRolesExist_shouldNotCreateDuplicates() {
        DataInitializer dataInitializer = new DataInitializer(roleRepository);
        when(roleRepository.existsByName("USER")).thenReturn(true);
        when(roleRepository.existsByName("ADMIN")).thenReturn(true);

        dataInitializer.run();

        verify(roleRepository, never()).save(org.mockito.ArgumentMatchers.any(Role.class));
    }
}
