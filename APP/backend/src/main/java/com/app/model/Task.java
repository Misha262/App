package com.app.model;

/**
 * Задача в учебной группе.
 */
public class Task {

    private int taskId;
    private int groupId;
    private int createdBy;
    private Integer assignedTo;    // может быть null
    private String title;
    private String description;
    private String status;         // OPEN, IN_PROGRESS, DONE
    private String deadline;       // строка DATETIME из SQLite
    private String priority;       // LOW, MEDIUM, HIGH
    private String createdAt;
    private String updatedAt;

    public Task(int taskId,
                int groupId,
                int createdBy,
                Integer assignedTo,
                String title,
                String description,
                String status,
                String deadline,
                String priority,
                String createdAt,
                String updatedAt) {
        this.taskId = taskId;
        this.groupId = groupId;
        this.createdBy = createdBy;
        this.assignedTo = assignedTo;
        this.title = title;
        this.description = description;
        this.status = status;
        this.deadline = deadline;
        this.priority = priority;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getTaskId() {
        return taskId;
    }

    public int getGroupId() {
        return groupId;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public Integer getAssignedTo() {
        return assignedTo;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getDeadline() {
        return deadline;
    }

    public String getPriority() {
        return priority;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
