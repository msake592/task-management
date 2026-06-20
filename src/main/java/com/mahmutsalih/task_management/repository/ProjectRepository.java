package com.mahmutsalih.task_management.repository;

import com.mahmutsalih.task_management.entity.Project;
import com.mahmutsalih.task_management.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findByOwner(User owner, Pageable pageable);
}
