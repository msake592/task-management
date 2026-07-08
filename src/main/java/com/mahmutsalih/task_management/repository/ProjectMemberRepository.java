package com.mahmutsalih.task_management.repository;

import com.mahmutsalih.task_management.entity.ProjectMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    List<ProjectMember> findByProjectId(Long projectId);

    @Query("select pm from ProjectMember pm join fetch pm.user where pm.project.id = :projectId")
    List<ProjectMember> findByProjectIdWithUser(@Param("projectId") Long projectId);

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);
}
