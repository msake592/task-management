package com.mahmutsalih.task_management.security;

import com.mahmutsalih.task_management.entity.Role;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(getAuthorities(user.getRole()))
                .build();
    }

    private List<GrantedAuthority> getAuthorities(Role role) {
        if (role == null || role.getName() == null || role.getName().isBlank()) {
            return Collections.emptyList();
        }

        String roleName = role.getName().startsWith("ROLE_") ? role.getName() : "ROLE_" + role.getName();
        return List.of(new SimpleGrantedAuthority(roleName));
    }
}
