package com.mahmutsalih.task_management.service;

import com.mahmutsalih.task_management.dto.request.CommentRequest;
import com.mahmutsalih.task_management.dto.response.CommentResponse;
import java.util.List;

public interface CommentService {

    CommentResponse addCommentToTask(Long taskId, CommentRequest request);

    List<CommentResponse> getCommentsByTaskId(Long taskId);

    void delete(Long id);
}
