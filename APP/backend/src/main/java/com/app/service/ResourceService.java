package com.app.service;

import com.app.model.Resource;
import com.app.repository.ResourceRepository;

import java.sql.SQLException;
import java.util.List;

public class ResourceService {

    private final ResourceRepository resourceRepo = new ResourceRepository();

    public List<Resource> getResourcesForGroup(int groupId) throws SQLException {
        return resourceRepo.findByGroupId(groupId);
    }

    public Resource getResource(int resourceId) throws SQLException {
        return resourceRepo.findById(resourceId);
    }

    /** Добавить ресурс-ссылку (LINK). */
    public Resource addLinkResource(int groupId,
                                    int uploadedBy,
                                    String title,
                                    String url,
                                    String description) throws Exception {

        if (groupId <= 0) throw new IllegalArgumentException("groupId is required");
        if (uploadedBy <= 0) throw new IllegalArgumentException("uploadedBy is required");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title is required");
        if (url == null || url.isBlank()) throw new IllegalArgumentException("url is required");

        return resourceRepo.createResource(
                groupId,
                uploadedBy,
                title.trim(),
                "LINK",
                url.trim(),
                null,
                null,
                description == null ? null : description.trim()
        );
    }

    /** Добавить ресурс-файл (FILE) – путь к файлу уже должен быть сформирован. */
    public Resource addFileResource(int groupId,
                                    int uploadedBy,
                                    String title,
                                    String filePath,
                                    String originalName,
                                    long fileSize,
                                    String description) throws Exception {

        if (groupId <= 0) throw new IllegalArgumentException("groupId is required");
        if (uploadedBy <= 0) throw new IllegalArgumentException("uploadedBy is required");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title is required");
        if (filePath == null || filePath.isBlank()) throw new IllegalArgumentException("filePath is required");

        return resourceRepo.createResource(
                groupId,
                uploadedBy,
                title.trim(),
                "FILE",
                filePath.trim(),
                originalName,
                fileSize,
                description == null ? null : description.trim()
        );
    }

    public void deleteResource(int resourceId) throws SQLException {
        resourceRepo.deleteResource(resourceId);
    }
}
