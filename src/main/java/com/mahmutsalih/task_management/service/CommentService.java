package com.mahmutsalih.task_management.service;

import java.util.List;

import com.mahmutsalih.task_management.dto.request.CreateCommentRequest;
import com.mahmutsalih.task_management.dto.response.CommentResponse;

public interface CommentService {

    CommentResponse addComment(Long taskId, Long userId, CreateCommentRequest request);

    List<CommentResponse> getCommentsByTask(Long taskId);
}
