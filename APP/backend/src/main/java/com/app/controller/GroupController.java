package com.app.controller;

import com.app.model.Group;
import com.app.service.ActivityService;
import com.app.service.GroupService;
import com.app.service.MembershipService;
import com.app.websocket.ChatWebSocketHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService = new GroupService();
    private final MembershipService membershipService = new MembershipService();
    private final ActivityService activityService = new ActivityService();
    private final ChatWebSocketHandler chatSocket;

    public GroupController(ChatWebSocketHandler chatSocket) {
        this.chatSocket = chatSocket;
    }

    @GetMapping
    public ResponseEntity<?> getGroups(@RequestAttribute("userId") Integer userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            List<Group> groups = groupService.getGroupsForUser(userId);
            return ResponseEntity.ok(groups);
        } catch (SQLException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Database error: " + e.getMessage()));
        }
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroup(@PathVariable int groupId,
                                      @RequestAttribute("userId") Integer userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            if (!membershipService.isMember(userId, groupId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not a member of this group"));
            }

            Group group = groupService.getGroup(groupId);
            if (group == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Group not found"));
            }

            return ResponseEntity.ok(group);
        } catch (SQLException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Database error: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestAttribute("userId") Integer userId,
                                         @RequestBody Map<String, Object> body) {
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            String name = (String) body.get("name");
            String description = (String) body.get("description");

            Group created = groupService.createGroup(userId, name, description);

            activityService.log(userId, "GROUP_CREATED",
                    Map.of("groupId", created.getGroupId(), "name", created.getName()));

            chatSocket.broadcastEvent(created.getGroupId(), Map.of(
                    "type", "EVENT",
                    "event", "GROUP_CREATED",
                    "groupId", created.getGroupId(),
                    "name", created.getName()
            ));

            return ResponseEntity.status(201).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SQLException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Database error: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<?> updateGroup(@PathVariable int groupId,
                                         @RequestAttribute("userId") Integer userId,
                                         @RequestBody Map<String, Object> body) {
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            if (!membershipService.isOwner(userId, groupId)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Only group OWNER can update group"));
            }

            String name = (String) body.get("name");
            String description = (String) body.get("description");

            groupService.updateGroup(groupId, name, description);

            activityService.log(userId, "GROUP_UPDATED",
                    Map.of("groupId", groupId, "name", name));

            chatSocket.broadcastEvent(groupId, Map.of(
                    "type", "EVENT",
                    "event", "GROUP_UPDATED",
                    "groupId", groupId,
                    "name", name
            ));

            return ResponseEntity.ok(Map.of("message", "Group updated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SQLException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Database error: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable int groupId,
                                         @RequestAttribute("userId") Integer userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            if (!membershipService.isOwner(userId, groupId)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Only group OWNER can delete group"));
            }

            groupService.deleteGroup(groupId);

            activityService.log(userId, "GROUP_DELETED", Map.of("groupId", groupId));

            chatSocket.broadcastEvent(groupId, Map.of(
                    "type", "EVENT",
                    "event", "GROUP_DELETED",
                    "groupId", groupId
            ));

            return ResponseEntity.noContent().build();
        } catch (SQLException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Database error: " + e.getMessage()));
        }
    }
}
