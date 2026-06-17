package com.mahmutsalih.task_management.repository;

import com.mahmutsalih.task_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
