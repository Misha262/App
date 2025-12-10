package com.app.model;

/**
 * DTO that combines group info with a couple of derived values.
 */
public class Group {

    private int groupId;
    private String name;
    private String description;
    private int createdBy;
    private String createdAt;
    private int memberCount;
    private int openTasksCount;

    public Group(int groupId,
                 String name,
                 String description,
                 int createdBy,
                 String createdAt,
                 int memberCount,
                 int openTasksCount) {
        this.groupId = groupId;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.memberCount = memberCount;
        this.openTasksCount = openTasksCount;
    }

    public int getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public int getOpenTasksCount() {
        return openTasksCount;
    }
}
