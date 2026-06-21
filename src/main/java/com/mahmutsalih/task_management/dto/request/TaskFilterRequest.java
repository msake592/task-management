package com.mahmutsalih.task_management.dto.request;

import com.mahmutsalih.task_management.enums.TaskPriority;
import com.mahmutsalih.task_management.enums.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskFilterRequest {

    private TaskStatus status;

    private TaskPriority priority;

    private Long assignedUserId;

    private Long projectId;

    private String keyword;

    @Min(value = 0, message = "Page cannot be negative")
    @Builder.Default
    private Integer page = 0;

    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 100, message = "Size cannot be greater than 100")
    @Builder.Default
    private Integer size = 10;

    @Pattern(
            regexp = "id|title|status|priority|createdAt|dueDate",
            message = "Sort field must be one of: id, title, status, priority, createdAt, dueDate"
    )
    @Builder.Default
    private String sortBy = "createdAt";

    @Pattern(regexp = "(?i)asc|desc", message = "Direction must be asc or desc")
    @Builder.Default
    private String direction = "desc";
}
