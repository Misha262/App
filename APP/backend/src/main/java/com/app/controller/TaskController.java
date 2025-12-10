package com.app.controller;

import com.app.model.Task;
import com.app.security.RoleGuard;
import com.app.service.ActivityService;
import com.app.service.TaskService;
import com.app.websocket.ChatWebSocketHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService = new TaskService();
    private final ActivityService activityService = new ActivityService();
    private final ChatWebSocketHandler chat;

    public TaskController(ChatWebSocketHandler chat) {
        this.chat = chat;
    }

    @GetMapping
    public ResponseEntity<?> getTasks(@RequestParam Integer groupId,
                                      @RequestAttribute("userId") Integer userId) throws Exception {

        RoleGuard.requireMember(userId, groupId);
        List<Task> list = taskService.getTasksForGroup(groupId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTask(@PathVariable Integer taskId,
                                     @RequestAttribute("userId") Integer userId) throws Exception {

        Task t = taskService.getTask(taskId);
        if (t == null) return ResponseEntity.notFound().build();

        RoleGuard.requireMember(userId, t.getGroupId());
        return ResponseEntity.ok(t);
    }

    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Map<String, Object> body,
                                        @RequestAttribute("userId") Integer userId) throws Exception {

        Integer groupId = (Integer) body.get("groupId");
        Integer assignedTo = (Integer) body.get("assignedTo");

        RoleGuard.requireMember(userId, groupId);

        Task created = taskService.createTask(
                groupId,
                userId,
                assignedTo,
                (String) body.get("title"),
                (String) body.get("description"),
                (String) body.get("status"),
                (String) body.get("deadline"),
                (String) body.get("priority")
        );

        activityService.log(userId, "TASK_CREATED",
                Map.of("taskId", created.getTaskId(), "groupId", groupId));

        chat.broadcastEvent(groupId, Map.of(
                "type", "EVENT",
                "event", "TASK_CREATED",
                "taskId", created.getTaskId(),
                "title", created.getTitle()
        ));

        return ResponseEntity.status(201).body(created);
    }

    @PostMapping("/{taskId}/status")
    public ResponseEntity<?> changeStatus(@PathVariable Integer taskId,
                                          @RequestBody Map<String, Object> body,
                                          @RequestAttribute("userId") Integer userId) throws Exception {

        Task t = taskService.getTask(taskId);
        if (t == null) return ResponseEntity.notFound().build();

        RoleGuard.requireMember(userId, t.getGroupId());

        String newStatus = (String) body.get("status");
        taskService.updateStatus(taskId, newStatus);

        activityService.log(userId, "TASK_STATUS_CHANGED",
                Map.of("taskId", taskId, "status", newStatus));

        chat.broadcastEvent(t.getGroupId(), Map.of(
                "type", "EVENT",
                "event", "TASK_STATUS_CHANGED",
                "taskId", taskId,
                "status", newStatus
        ));

        return ResponseEntity.ok(Map.of("message", "Status updated"));
    }

    // =========================
    // Resource attachments
    // =========================
    @GetMapping("/{taskId}/resources")
    public ResponseEntity<?> getTaskResources(@PathVariable Integer taskId,
                                              @RequestAttribute("userId") Integer userId) throws Exception {
        Task t = taskService.getTask(taskId);
        if (t == null) return ResponseEntity.notFound().build();
        RoleGuard.requireMember(userId, t.getGroupId());

        List<Integer> ids = taskService.getResourceIds(taskId);
        return ResponseEntity.ok(ids);
    }

    @PostMapping("/{taskId}/resources")
    public ResponseEntity<?> attachResource(@PathVariable Integer taskId,
                                            @RequestBody Map<String, Object> body,
                                            @RequestAttribute("userId") Integer userId) throws Exception {

        Task t = taskService.getTask(taskId);
        if (t == null) return ResponseEntity.notFound().build();

        RoleGuard.requireMember(userId, t.getGroupId());
        Integer resourceId = (Integer) body.get("resourceId");
        if (resourceId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "resourceId is required"));
        }

        taskService.attachResource(taskId, resourceId, userId);

        chat.broadcastEvent(t.getGroupId(), Map.of(
                "type", "EVENT",
                "event", "TASK_RESOURCE_ATTACHED",
                "taskId", taskId,
                "resourceId", resourceId
        ));

        return ResponseEntity.ok(Map.of("message", "Resource attached"));
    }

    @DeleteMapping("/{taskId}/resources/{resourceId}")
    public ResponseEntity<?> detachResource(@PathVariable Integer taskId,
                                            @PathVariable Integer resourceId,
                                            @RequestAttribute("userId") Integer userId) throws Exception {

        Task t = taskService.getTask(taskId);
        if (t == null) return ResponseEntity.notFound().build();

        RoleGuard.requireMember(userId, t.getGroupId());
        taskService.detachResource(taskId, resourceId, userId);

        chat.broadcastEvent(t.getGroupId(), Map.of(
                "type", "EVENT",
                "event", "TASK_RESOURCE_DETACHED",
                "taskId", taskId,
                "resourceId", resourceId
        ));

        return ResponseEntity.ok(Map.of("message", "Resource detached"));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<?> updateTask(@PathVariable Integer taskId,
                                        @RequestBody Map<String, Object> body,
                                        @RequestAttribute("userId") Integer userId) throws Exception {

        Task t = taskService.getTask(taskId);
        if (t == null) return ResponseEntity.notFound().build();

        int groupId = t.getGroupId();

        boolean canEdit =
                t.getCreatedBy() == userId ||
                        RoleGuard.isAdminOrOwner(userId, groupId);

        if (!canEdit) {
            return ResponseEntity.status(403).body(Map.of("error", "No permission"));
        }

        taskService.updateTask(
                taskId,
                (Integer) body.get("assignedTo"),
                (String) body.get("title"),
                (String) body.get("description"),
                (String) body.get("deadline"),
                (String) body.get("priority")
        );

        activityService.log(userId, "TASK_UPDATED",
                Map.of("taskId", taskId));

        chat.broadcastEvent(groupId, Map.of(
                "type", "EVENT",
                "event", "TASK_UPDATED",
                "taskId", taskId
        ));

        return ResponseEntity.ok(Map.of("message", "Task updated"));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Integer taskId,
                                        @RequestAttribute("userId") Integer userId) throws Exception {

        Task t = taskService.getTask(taskId);
        if (t == null) return ResponseEntity.notFound().build();

        int groupId = t.getGroupId();

        boolean canDelete =
                t.getCreatedBy() == userId ||
                        RoleGuard.isAdminOrOwner(userId, groupId);

        if (!canDelete) {
            return ResponseEntity.status(403).body(Map.of("error", "No permission"));
        }

        taskService.deleteTask(taskId);

        activityService.log(userId, "TASK_DELETED",
                Map.of("taskId", taskId));

        chat.broadcastEvent(groupId, Map.of(
                "type", "EVENT",
                "event", "TASK_DELETED",
                "taskId", taskId
        ));

        return ResponseEntity.noContent().build();
    }
}
