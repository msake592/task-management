package com.mahmutsalih.task_management.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mahmutsalih.task_management.controller.AuthController;
import com.mahmutsalih.task_management.controller.UserController;
import com.mahmutsalih.task_management.dto.request.RegisterRequest;
import com.mahmutsalih.task_management.dto.request.UserRequest;
import com.mahmutsalih.task_management.dto.response.UserResponse;
import com.mahmutsalih.task_management.service.AuthService;
import com.mahmutsalih.task_management.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@WebMvcTest({UserController.class, AuthController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserEndpointSecurityTest {

    private static final String USER_JSON = """
            {
              "firstName": "Managed",
              "lastName": "User",
              "email": "managed@example.com",
              "password": "password",
              "roleId": 2
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createUser_withUserRole_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(user("user@example.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USER_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_withAdminRole_shouldReturnCreated() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(10L)
                .firstName("Managed")
                .lastName("User")
                .email("managed@example.com")
                .roleId(2L)
                .roleName("ADMIN")
                .build();
        when(userService.create(any(UserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roleName").value("ADMIN"));
    }

    @Test
    void getUserOptions_withUserRole_shouldReturnOk() throws Exception {
        when(userService.getAll(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/users/options")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void register_withoutAuthentication_shouldReturnCreatedUserRole() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(11L)
                .firstName("Public")
                .lastName("User")
                .email("public@example.com")
                .roleId(1L)
                .roleName("USER")
                .build();
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Public",
                                  "lastName": "User",
                                  "email": "public@example.com",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roleName").value("USER"));
    }
}
