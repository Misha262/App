package com.app.model;

/**
 * DTO representing uploaded files/links in a group.
 */
public class Resource {

    private int resourceId;
    private int groupId;
    private int uploadedBy;
    private String title;
    private String type;
    private String pathOrUrl;
    private String uploadedAt;
    private String originalName;
    private Long fileSize;
    private String description;
    private String uploaderName;
    private String uploaderEmail;

    public Resource(int resourceId,
                    int groupId,
                    int uploadedBy,
                    String title,
                    String type,
                    String pathOrUrl,
                    String uploadedAt,
                    String originalName,
                    Long fileSize,
                    String description,
                    String uploaderName,
                    String uploaderEmail) {
        this.resourceId = resourceId;
        this.groupId = groupId;
        this.uploadedBy = uploadedBy;
        this.title = title;
        this.type = type;
        this.pathOrUrl = pathOrUrl;
        this.uploadedAt = uploadedAt;
        this.originalName = originalName;
        this.fileSize = fileSize;
        this.description = description;
        this.uploaderName = uploaderName;
        this.uploaderEmail = uploaderEmail;
    }

    public int getResourceId() {
        return resourceId;
    }

    public int getGroupId() {
        return groupId;
    }

    public int getUploadedBy() {
        return uploadedBy;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getPathOrUrl() {
        return pathOrUrl;
    }

    public String getUploadedAt() {
        return uploadedAt;
    }

    public String getOriginalName() {
        return originalName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getDescription() {
        return description;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public String getUploaderEmail() {
        return uploaderEmail;
    }
}
