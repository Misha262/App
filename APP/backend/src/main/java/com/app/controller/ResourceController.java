package com.app.controller;

import com.app.model.Resource;
import com.app.security.RoleGuard;
import com.app.service.ActivityService;
import com.app.service.GcsStorageService;
import com.app.service.MembershipService;
import com.app.service.ResourceService;
import com.app.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService = new ResourceService();
    private final MembershipService membershipService = new MembershipService();
    private final ActivityService activityService = new ActivityService();
    private final ChatWebSocketHandler chatSocket;

    private final GcsStorageService gcs;

    public ResourceController(ChatWebSocketHandler chatSocket,
                              @Value("${storage.bucket:}") String bucket) {
        this.chatSocket = chatSocket;
        this.gcs = (bucket == null || bucket.isBlank()) ? null : new GcsStorageService(bucket);
    }

    @GetMapping
    public ResponseEntity<?> listResources(@RequestParam int groupId,
                                           @RequestAttribute("userId") Integer userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            if (!membershipService.isMember(userId, groupId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not a group member"));
            }

            List<Resource> data = resourceService.getResourcesForGroup(groupId);
            return ResponseEntity.ok(data);

        } catch (SQLException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Database error: " + e.getMessage()));
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam int groupId,
                                    @RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "title", required = false) String title,
                                    @RequestParam(value = "description", required = false) String description,
                                    @RequestAttribute("userId") Integer userId) {

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is required"));
        }

        try {
            RoleGuard.requireMember(userId, groupId);

            if (gcs == null) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Storage bucket is not configured"));
            }

            String originalName = file.getOriginalFilename();
            if (title == null || title.isBlank()) {
                title = (originalName == null || originalName.isBlank()) ? "File" : originalName;
            }

            String objectName = gcs.uploadFile(groupId, file);

            Resource saved = resourceService.addFileResource(
                    groupId,
                    userId,
                    title,
                    objectName,
                    originalName,
                    file.getSize(),
                    description
            );

            activityService.log(userId, "RESOURCE_UPLOADED",
                    Map.of("groupId", groupId, "resourceId", saved.getResourceId(), "title", saved.getTitle()));

            chatSocket.broadcastEvent(groupId, Map.of(
                    "type", "EVENT",
                    "event", "RESOURCE_UPLOADED",
                    "resourceId", saved.getResourceId(),
                    "title", saved.getTitle()
            ));

            return ResponseEntity.status(201).body(saved);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{resourceId}/download")
    public ResponseEntity<?> download(@PathVariable int resourceId,
                                      @RequestAttribute("userId") Integer userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            Resource resource = resourceService.getResource(resourceId);
            if (resource == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Resource not found"));
            }

            RoleGuard.requireMember(userId, resource.getGroupId());

            if (!"FILE".equalsIgnoreCase(resource.getType())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Not a downloadable file"));
            }

            if (gcs == null) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Storage bucket is not configured"));
            }

            InputStream stream = gcs.download(resource.getPathOrUrl());
            if (stream == null) {
                return ResponseEntity.status(404).body(Map.of("error", "File missing on server"));
            }

            InputStreamResource body = new InputStreamResource(stream);

            String downloadName = resource.getOriginalName() != null
                    ? resource.getOriginalName()
                    : UUID.randomUUID().toString();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                    .body(body);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<?> delete(@PathVariable int resourceId,
                                    @RequestAttribute("userId") Integer userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            Resource resource = resourceService.getResource(resourceId);
            if (resource == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Resource not found"));
            }

            if (!RoleGuard.isAdminOrOwner(userId, resource.getGroupId()) && resource.getUploadedBy() != userId) {
                return ResponseEntity.status(403).body(Map.of("error", "No permission to delete resource"));
            }

            resourceService.deleteResource(resourceId);
            if ("FILE".equalsIgnoreCase(resource.getType()) && gcs != null) {
                gcs.delete(resource.getPathOrUrl());
            }

            activityService.log(userId, "RESOURCE_DELETED",
                    Map.of("groupId", resource.getGroupId(), "resourceId", resourceId));

            chatSocket.broadcastEvent(resource.getGroupId(), Map.of(
                    "type", "EVENT",
                    "event", "RESOURCE_DELETED",
                    "resourceId", resourceId
            ));

            return ResponseEntity.noContent().build();

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

}
