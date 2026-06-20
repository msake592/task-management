package com.mahmutsalih.task_management.security;

import com.mahmutsalih.task_management.entity.Project;
import com.mahmutsalih.task_management.entity.Role;
import com.mahmutsalih.task_management.entity.User;
import com.mahmutsalih.task_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private static final String ACCESS_DENIED_MESSAGE = "You do not have permission to access this resource";

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException(ACCESS_DENIED_MESSAGE));
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> isAdminRole(authority.getAuthority()))) {
            return true;
        }

        return isAdmin(getCurrentUser());
    }

    public void validateProjectAccess(Project project) {
        if (!canAccessProject(project)) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }

    public boolean canAccessProject(Project project) {
        if (isAdmin()) {
            return true;
        }

        User currentUser = getCurrentUser();
        User owner = project.getOwner();
        return owner != null && owner.getId() != null && owner.getId().equals(currentUser.getId());
    }

    public boolean isAdmin(User user) {
        if (user == null) {
            return false;
        }

        Role role = user.getRole();
        return role != null && isAdminRole(role.getName());
    }

    private boolean isAdminRole(String roleName) {
        if (roleName == null) {
            return false;
        }

        return "ADMIN".equals(roleName) || "ROLE_ADMIN".equals(roleName);
    }
}
