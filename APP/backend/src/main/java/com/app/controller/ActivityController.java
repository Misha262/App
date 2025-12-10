package com.app.controller;

import com.app.model.Activity;
import com.app.security.RoleGuard;
import com.app.service.ActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityService activityService = new ActivityService();

    // ================================================================
    // GET /api/activity/me
    // ================================================================
    @GetMapping("/me")
    public ResponseEntity<?> getMyActivity(
            @RequestParam(defaultValue = "50") int limit,
            @RequestAttribute("userId") int userId
    ) {
        try {
            limit = normalizeLimit(limit);
            List<Activity> list = activityService.recentForUser(userId, limit);
            return ResponseEntity.ok(list);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // ================================================================
    // GET /api/activity/group/{groupId}
    // ================================================================
    @GetMapping("/group/{groupId}")
    public ResponseEntity<?> getGroupActivity(
            @PathVariable int groupId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestAttribute("userId") int userId
    ) {
        try {
            RoleGuard.requireMember(userId, groupId);

            limit = normalizeLimit(limit);

            List<Activity> list = activityService.recentForGroup(groupId, limit);
            return ResponseEntity.ok(list);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));

        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Database error: " + e.getMessage())
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // ================================================================
    // Helper
    // ================================================================
    private int normalizeLimit(int limit) {
        if (limit <= 0) return 50;
        return Math.min(limit, 200);
    }
}
