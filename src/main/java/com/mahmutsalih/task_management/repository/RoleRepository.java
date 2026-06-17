package com.mahmutsalih.task_management.repository;

import com.mahmutsalih.task_management.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
