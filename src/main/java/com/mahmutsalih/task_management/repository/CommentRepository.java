package com.mahmutsalih.task_management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mahmutsalih.task_management.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTaskId(Long taskId);
}
