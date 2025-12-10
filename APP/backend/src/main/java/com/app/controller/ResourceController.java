package com.app.controller;

import com.app.model.Resource;
import com.app.security.RoleGuard;
import com.app.service.ActivityService;
import com.app.service.MembershipService;
import com.app.service.ResourceService;
import com.app.websocket.ChatWebSocketHandler;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final Path storageRoot = Paths.get("uploads", "resources");

    public ResourceController(ChatWebSocketHandler chatSocket) throws IOException {
        this.chatSocket = chatSocket;
        Files.createDirectories(storageRoot);
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

            String originalName = file.getOriginalFilename();
            if (title == null || title.isBlank()) {
                title = (originalName == null || originalName.isBlank()) ? "File" : originalName;
            }

            String storedName = UUID.randomUUID().toString().replace("-", "");
            if (originalName != null && originalName.contains(".")) {
                storedName += originalName.substring(originalName.lastIndexOf('.'));
            }

            Path dir = storageRoot.resolve("group-" + groupId);
            Files.createDirectories(dir);

            Path target = dir.resolve(storedName);
            file.transferTo(target);

            Resource saved = resourceService.addFileResource(
                    groupId,
                    userId,
                    title,
                    storageRoot.relativize(target).toString(),
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

            Path filePath = resolveStoragePath(resource.getPathOrUrl());
            if (!Files.exists(filePath)) {
                return ResponseEntity.status(404).body(Map.of("error", "File missing on server"));
            }

            InputStreamResource body = new InputStreamResource(Files.newInputStream(filePath));

            String downloadName = resource.getOriginalName() != null
                    ? resource.getOriginalName()
                    : filePath.getFileName().toString();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                    .contentLength(Files.size(filePath))
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
            deleteStoredFile(resource);

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

    private Path resolveStoragePath(String stored) {
        Path path = Paths.get(stored);
        if (path.isAbsolute()) return path;
        return storageRoot.resolve(path);
    }

    private void deleteStoredFile(Resource resource) {
        if (!"FILE".equalsIgnoreCase(resource.getType())) {
            return;
        }
        try {
            Path path = resolveStoragePath(resource.getPathOrUrl());
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
