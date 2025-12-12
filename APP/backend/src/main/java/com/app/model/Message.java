package com.app.model;

public class Message {
    private final int messageId;
    private final int groupId;
    private final int userId;
    private final String userName;
    private final String content;
    private final Integer resourceId;
    private final String resourceTitle;
    private final Integer taskId;
    private final String timestamp;

    public Message(int messageId, int groupId, int userId, String userName, String content,
                   Integer resourceId, String resourceTitle, Integer taskId, String timestamp) {
        this.messageId = messageId;
        this.groupId = groupId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.resourceId = resourceId;
        this.resourceTitle = resourceTitle;
        this.taskId = taskId;
        this.timestamp = timestamp;
    }

    public int getMessageId() {
        return messageId;
    }

    public int getGroupId() {
        return groupId;
    }

    public int getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getContent() {
        return content;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public String getResourceTitle() {
        return resourceTitle;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
