package com.mahmutsalih.task_management.service;

import com.mahmutsalih.task_management.dto.request.UserRequest;
import com.mahmutsalih.task_management.dto.response.UserResponse;
import java.util.List;

public interface UserService {

    UserResponse create(UserRequest request);

    List<UserResponse> getAll();

    UserResponse getById(Long id);

    UserResponse update(Long id, UserRequest request);

    void delete(Long id);
}
