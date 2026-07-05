package com.mahmutsalih.task_management.repository;

import com.mahmutsalih.task_management.entity.TaskAssignment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    @EntityGraph(attributePaths = "user")
    List<TaskAssignment> findByTaskId(Long taskId);

    List<TaskAssignment> findByUserId(Long userId);

    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    void deleteByTaskId(Long taskId);
}
