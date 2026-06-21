package com.mahmutsalih.task_management.config;

import com.mahmutsalih.task_management.entity.Role;
import com.mahmutsalih.task_management.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_USER_ROLE = "USER";

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        if (!roleRepository.existsByName(DEFAULT_USER_ROLE)) {
            roleRepository.save(Role.builder().name(DEFAULT_USER_ROLE).build());
        }
    }
}
