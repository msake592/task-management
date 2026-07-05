package com.mahmutsalih.task_management.dto.request;

import com.mahmutsalih.task_management.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddProjectMemberRequest {

    @NotNull(message = "User id is required")
    private Long userId;

    private ProjectRole role;
}
