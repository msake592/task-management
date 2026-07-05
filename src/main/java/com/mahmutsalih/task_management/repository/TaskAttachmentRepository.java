package com.mahmutsalih.task_management.repository;

import com.mahmutsalih.task_management.entity.TaskAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {

    List<TaskAttachment> findByTaskId(Long taskId);

    Optional<TaskAttachment> findByIdAndTaskId(Long id, Long taskId);
}
