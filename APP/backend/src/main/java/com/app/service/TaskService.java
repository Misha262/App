package com.app.service;

import com.app.model.Resource;
import com.app.model.Task;
import com.app.repository.TaskRepository;
import com.app.repository.TaskResourceRepository;

import java.sql.SQLException;
import java.util.List;

public class TaskService {

    private final TaskRepository taskRepo = new TaskRepository();
    private final TaskResourceRepository taskResourceRepo = new TaskResourceRepository();
    private final ResourceService resourceService = new ResourceService();
    private final MembershipService membershipService = new MembershipService();

    public List<Task> getTasksForGroup(int groupId) throws SQLException {
        return taskRepo.findByGroupId(groupId);
    }

    public Task getTask(int taskId) throws SQLException {
        return taskRepo.findById(taskId);
    }

    public Task createTask(int groupId,
                           int createdBy,
                           Integer assignedTo,
                           String title,
                           String description,
                           String status,
                           String deadline,
                           String priority) throws Exception {

        if (groupId <= 0) {
            throw new IllegalArgumentException("groupId is required");
        }
        if (createdBy <= 0) {
            throw new IllegalArgumentException("createdBy is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }

        if (description == null) {
            description = "";
        }

        if (status == null || status.isBlank()) {
            status = "OPEN";
        }

        status = status.toUpperCase();
        if (!status.equals("OPEN") &&
            !status.equals("IN_PROGRESS") &&
            !status.equals("DONE")) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        if (priority == null || priority.isBlank()) {
            priority = "MEDIUM";
        }

        priority = priority.toUpperCase();
        if (!priority.equals("LOW") &&
            !priority.equals("MEDIUM") &&
            !priority.equals("HIGH")) {
            throw new IllegalArgumentException("Invalid priority: " + priority);
        }

        return taskRepo.createTask(
                groupId,
                createdBy,
                assignedTo,
                title.trim(),
                description.trim(),
                status,
                deadline,
                priority
        );
    }

    public void updateTask(int taskId,
                           Integer assignedTo,
                           String title,
                           String description,
                           String deadline,
                           String priority) throws Exception {

        if (taskId <= 0) {
            throw new IllegalArgumentException("taskId is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (description == null) {
            description = "";
        }

        if (priority == null || priority.isBlank()) {
            priority = "MEDIUM";
        }

        priority = priority.toUpperCase();
        if (!priority.equals("LOW") &&
            !priority.equals("MEDIUM") &&
            !priority.equals("HIGH")) {
            throw new IllegalArgumentException("Invalid priority: " + priority);
        }

        taskRepo.updateTask(
                taskId,
                assignedTo,
                title.trim(),
                description.trim(),
                deadline,
                priority
        );
    }

    public void updateStatus(int taskId, String status) throws Exception {
        if (taskId <= 0) {
            throw new IllegalArgumentException("taskId is required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }

        status = status.toUpperCase();
        if (!status.equals("OPEN") &&
            !status.equals("IN_PROGRESS") &&
            !status.equals("DONE")) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        taskRepo.updateStatus(taskId, status);
    }

    public void deleteTask(int taskId) throws SQLException {
        taskRepo.deleteTask(taskId);
    }

    // =========================
    // Attachments (resources)
    // =========================
    public List<Integer> getResourceIds(int taskId) throws SQLException {
        return taskResourceRepo.findResourceIdsForTask(taskId);
    }

    public void attachResource(int taskId, int resourceId, int actorUserId) throws Exception {
        Task task = getTask(taskId);
        if (task == null) throw new IllegalArgumentException("Task not found");

        Resource res = resourceService.getResource(resourceId);
        if (res == null) throw new IllegalArgumentException("Resource not found");

        if (task.getGroupId() != res.getGroupId()) {
            throw new IllegalArgumentException("Resource and task must belong to the same group");
        }

        if (!membershipService.isMember(actorUserId, task.getGroupId())) {
            throw new IllegalArgumentException("Not a member of this group");
        }

        taskResourceRepo.attach(taskId, resourceId);
    }

    public void detachResource(int taskId, int resourceId, int actorUserId) throws Exception {
        Task task = getTask(taskId);
        if (task == null) throw new IllegalArgumentException("Task not found");

        if (!membershipService.isAdminOrOwner(actorUserId, task.getGroupId())
                && task.getCreatedBy() != actorUserId) {
            throw new IllegalArgumentException("No permission to detach resource");
        }

        taskResourceRepo.detach(taskId, resourceId);
    }
}
