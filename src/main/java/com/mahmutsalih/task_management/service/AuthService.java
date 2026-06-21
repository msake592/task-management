package com.mahmutsalih.task_management.service;

import com.mahmutsalih.task_management.dto.request.LoginRequest;
import com.mahmutsalih.task_management.dto.request.RegisterRequest;
import com.mahmutsalih.task_management.dto.response.AuthResponse;
import com.mahmutsalih.task_management.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
