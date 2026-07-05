package com.mahmutsalih.task_management.service;

import com.mahmutsalih.task_management.dto.request.AddProjectMemberRequest;
import com.mahmutsalih.task_management.dto.request.ProjectRequest;
import com.mahmutsalih.task_management.dto.response.ProjectMemberResponse;
import com.mahmutsalih.task_management.dto.response.ProjectResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectService {

    ProjectResponse create(ProjectRequest request);

    Page<ProjectResponse> getAll(Pageable pageable);

    ProjectResponse getById(Long id);

    ProjectResponse update(Long id, ProjectRequest request);

    void delete(Long id);

    ProjectMemberResponse addMember(Long projectId, AddProjectMemberRequest request);

    List<ProjectMemberResponse> getMembers(Long projectId);
}
