package com.mahmutsalih.task_management.service;

import com.mahmutsalih.task_management.dto.request.ProjectRequest;
import com.mahmutsalih.task_management.dto.response.ProjectResponse;
import java.util.List;

public interface ProjectService {

    ProjectResponse create(ProjectRequest request);

    List<ProjectResponse> getAll();

    ProjectResponse getById(Long id);

    ProjectResponse update(Long id, ProjectRequest request);

    void delete(Long id);
}
