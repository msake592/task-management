package com.mahmutsalih.task_management.repository;

import com.mahmutsalih.task_management.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
