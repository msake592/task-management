package com.mahmutsalih.task_management.service;

import com.mahmutsalih.task_management.dto.request.AddProjectMemberRequest;
import com.mahmutsalih.task_management.dto.request.ProjectRequest;
import com.mahmutsalih.task_management.dto.response.ProjectMemberResponse;
import com.mahmutsalih.task_management.dto.response.ProjectResponse;
import com.mahmutsalih.task_management.entity.Project;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectService {

    ProjectResponse create(ProjectRequest request);

    Page<ProjectResponse> getAll(Pageable pageable);

    ProjectResponse getById(Long id);

    Project getEntityById(Long id);

    boolean isMember(Long projectId, Long userId);

    ProjectResponse update(Long id, ProjectRequest request);

    void delete(Long id);

    ProjectMemberResponse addMember(Long projectId, AddProjectMemberRequest request);

    List<ProjectMemberResponse> getMembers(Long projectId);
}
