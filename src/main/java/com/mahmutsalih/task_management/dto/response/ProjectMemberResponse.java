package com.mahmutsalih.task_management.dto.response;

import com.mahmutsalih.task_management.enums.ProjectRole;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberResponse {

    private Long userId;
    private String name;
    private String email;
    private ProjectRole role;
    private LocalDateTime joinedAt;
}
