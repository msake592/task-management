package com.mahmutsalih.task_management.service;

import java.util.List;

import com.mahmutsalih.task_management.dto.request.CommentRequest;
import com.mahmutsalih.task_management.dto.response.CommentResponse;

public interface CommentService {

    CommentResponse addCommentToTask(Long taskId, CommentRequest request);

    List<CommentResponse> getCommentsByTaskId(Long taskId);

    void delete(Long id);
}
