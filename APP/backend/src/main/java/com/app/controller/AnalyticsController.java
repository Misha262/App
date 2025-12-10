package com.app.controller;

import com.app.model.Task;
import com.app.security.RoleGuard;
import com.app.service.MembershipService;
import com.app.service.ResourceService;
import com.app.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class AnalyticsController {

    private final TaskService taskService = new TaskService();
    private final MembershipService membershipService = new MembershipService();
    private final ResourceService resourceService = new ResourceService();

    @GetMapping("/api/analytics/summary")
    public ResponseEntity<?> summary(@RequestParam Integer groupId,
                                     @RequestAttribute("userId") Integer userId) throws Exception {
        RoleGuard.requireMember(userId, groupId);

        List<Task> tasks = taskService.getTasksForGroup(groupId);
        int members = membershipService.getMembersOfGroup(groupId).size();
        int resources = resourceService.getResourcesForGroup(groupId).size();

        long open = tasks.stream().filter(t -> "OPEN".equalsIgnoreCase(t.getStatus())).count();
        long inProgress = tasks.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus())).count();
        long done = tasks.stream().filter(t -> "DONE".equalsIgnoreCase(t.getStatus())).count();

        Instant now = Instant.now();
        long overdue = tasks.stream()
                .filter(t -> t.getDeadline() != null)
                .filter(t -> {
                    try {
                        Instant d = ZonedDateTime.parse(t.getDeadline()).toInstant();
                        return d.isBefore(now);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();

        Map<String, Long> perDay = tasks.stream()
                .filter(t -> t.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().substring(0, Math.min(10, t.getCreatedAt().length())),
                        Collectors.counting()
                ));

        Map<String, Object> payload = new HashMap<>();
        payload.put("groupId", groupId);
        payload.put("members", members);
        payload.put("resources", resources);
        payload.put("tasksTotal", tasks.size());
        payload.put("tasks", Map.of(
                "open", open,
                "inProgress", inProgress,
                "done", done,
                "overdue", overdue
        ));
        payload.put("tasksCreatedPerDay", perDay);

        return ResponseEntity.ok(payload);
    }
}
