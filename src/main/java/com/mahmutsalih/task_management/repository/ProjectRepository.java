package com.mahmutsalih.task_management.repository;

import com.mahmutsalih.task_management.entity.Project;
import com.mahmutsalih.task_management.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findByOwner(User owner, Pageable pageable);

    @Query(
            value = """
                    select p
                    from Project p
                    where p.owner = :user
                       or exists (
                           select 1
                           from Task t
                           where t.project = p
                             and t.assignedUser = :user
                       )
                    """,
            countQuery = """
                    select count(p)
                    from Project p
                    where p.owner = :user
                       or exists (
                           select 1
                           from Task t
                           where t.project = p
                             and t.assignedUser = :user
                       )
                    """
    )
    Page<Project> findVisibleToUser(@Param("user") User user, Pageable pageable);

    @Query("""
            select count(t) > 0
            from Task t
            where t.project.id = :projectId
              and t.assignedUser = :user
            """)
    boolean existsAssignedTaskInProject(@Param("projectId") Long projectId, @Param("user") User user);
}
