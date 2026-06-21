package com.mahmutsalih.task_management.config;

import com.mahmutsalih.task_management.entity.Role;
import com.mahmutsalih.task_management.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String USER_ROLE = "USER";
    private static final String ADMIN_ROLE = "ADMIN";

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        seedRole(USER_ROLE);
        seedRole(ADMIN_ROLE);
    }

    private void seedRole(String roleName) {
        if (!roleRepository.existsByName(roleName)) {
            roleRepository.save(Role.builder().name(roleName).build());
        }
    }
}
