package com.mahmutsalih.task_management.repository.specification;

import com.mahmutsalih.task_management.dto.request.TaskFilterRequest;
import com.mahmutsalih.task_management.entity.Task;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class TaskSpecification {

    private TaskSpecification() {
    }

    public static Specification<Task> withFilters(
            TaskFilterRequest filter,
            boolean admin,
            Long currentUserId
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getPriority() != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), filter.getPriority()));
            }

            if (filter.getProjectId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("project").get("id"), filter.getProjectId()));
            }

            if (filter.getAssignedUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("assignedUser").get("id"), filter.getAssignedUserId()));
            }

            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String keyword = "%" + filter.getKeyword().trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), keyword)
                ));
            }

            if (!admin) {
                predicates.add(criteriaBuilder.equal(root.get("project").get("owner").get("id"), currentUserId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
