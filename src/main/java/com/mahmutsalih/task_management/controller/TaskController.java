package com.mahmutsalih.task_management.controller;

import com.mahmutsalih.task_management.dto.request.TaskRequest;
import com.mahmutsalih.task_management.dto.request.TaskFilterRequest;
import com.mahmutsalih.task_management.dto.request.TaskUpdateRequest;
import com.mahmutsalih.task_management.dto.response.TaskResponse;
import com.mahmutsalih.task_management.enums.TaskStatus;
import com.mahmutsalih.task_management.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getAll(@Valid @ModelAttribute TaskFilterRequest filterRequest) {
        return ResponseEntity.ok(taskService.getAll(filterRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable Long id,
            @NotNull(message = "Task status is required") @RequestParam TaskStatus status
    ) {
        return ResponseEntity.ok(taskService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<TaskResponse> assignToUser(
            @PathVariable Long id,
            @NotNull(message = "User id is required") @RequestParam Long userId
    ) {
        return ResponseEntity.ok(taskService.assignToUser(id, userId));
    }
}
