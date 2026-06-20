package com.mahmutsalih.task_management.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mahmutsalih.task_management.entity.Project;
import com.mahmutsalih.task_management.entity.Role;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void canAccessProject_whenUserOwnsProject_shouldReturnTrue() {
        User currentUser = user(1L, "faruk@test.com", "USER");
        Project project = Project.builder().id(10L).owner(currentUser).build();
        CurrentUserService currentUserService = new CurrentUserService(userRepository);

        authenticate("faruk@test.com", "ROLE_USER");
        when(userRepository.findByEmail("faruk@test.com")).thenReturn(Optional.of(currentUser));

        assertThat(currentUserService.canAccessProject(project)).isTrue();
    }

    @Test
    void canAccessProject_whenUserDoesNotOwnProject_shouldReturnFalse() {
        User currentUser = user(1L, "ali@test.com", "USER");
        User owner = user(2L, "faruk@test.com", "USER");
        Project project = Project.builder().id(10L).owner(owner).build();
        CurrentUserService currentUserService = new CurrentUserService(userRepository);

        authenticate("ali@test.com", "ROLE_USER");
        when(userRepository.findByEmail("ali@test.com")).thenReturn(Optional.of(currentUser));

        assertThat(currentUserService.canAccessProject(project)).isFalse();
    }

    @Test
    void canAccessProject_whenUserIsAdmin_shouldReturnTrue() {
        Project project = Project.builder().id(10L).owner(user(2L, "faruk@test.com", "USER")).build();
        CurrentUserService currentUserService = new CurrentUserService(userRepository);

        authenticate("admin@test.com", "ROLE_ADMIN");

        assertThat(currentUserService.canAccessProject(project)).isTrue();
    }

    private void authenticate(String email, String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                )
        );
    }

    private User user(Long id, String email, String roleName) {
        return User.builder()
                .id(id)
                .email(email)
                .firstName("Test")
                .role(Role.builder().name(roleName).build())
                .build();
    }
}
