package com.mahmutsalih.task_management.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mahmutsalih.task_management.entity.Role;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void loadUserByUsername_whenUserRole_shouldGrantOnlyUserAuthority() {
        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user("user@example.com", "USER")));

        UserDetails userDetails = service.loadUserByUsername("user@example.com");

        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER")
                .doesNotContain("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_whenAdminRole_shouldGrantAdminAuthority() {
        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(user("admin@example.com", "ADMIN")));

        UserDetails userDetails = service.loadUserByUsername("admin@example.com");

        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    private User user(String email, String roleName) {
        return User.builder()
                .email(email)
                .password("encoded-password")
                .role(Role.builder().name(roleName).build())
                .build();
    }
}
