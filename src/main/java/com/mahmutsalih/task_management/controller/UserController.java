package com.mahmutsalih.task_management.controller;

import com.mahmutsalih.task_management.dto.request.UserRequest;
import com.mahmutsalih.task_management.dto.response.UserResponse;
import com.mahmutsalih.task_management.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAll(Pageable pageable, Authentication authentication) {
        Page<UserResponse> users = userService.getAll(pageable);
        logger.info("Admin listed users. adminUsername={}", authentication.getName());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/options")
    public ResponseEntity<Page<UserResponse>> getOptions(Pageable pageable) {
        return ResponseEntity.ok(userService.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request,
            Authentication authentication
    ) {
        UserResponse user = userService.update(id, request);
        logger.info("User role updated by admin. adminUsername={}, targetUserId={}, newRole={}",
                authentication.getName(), id, user.getRoleName());
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        userService.delete(id);
        logger.info("User deleted by admin. adminUsername={}, targetUserId={}", authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
