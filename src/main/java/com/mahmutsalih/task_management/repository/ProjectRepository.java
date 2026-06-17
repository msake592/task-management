package com.mahmutsalih.task_management.repository;

import com.mahmutsalih.task_management.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
