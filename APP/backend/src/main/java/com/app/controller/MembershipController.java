package com.app.controller;

import com.app.model.Membership;
import com.app.security.RoleGuard;
import com.app.service.MembershipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups/{groupId}/members")
public class MembershipController {

    private final MembershipService membershipService = new MembershipService();

    @GetMapping
    public ResponseEntity<?> listMembers(@PathVariable int groupId,
                                         @RequestAttribute("userId") Integer userId) throws Exception {
        RoleGuard.requireMember(userId, groupId);
        List<Membership> members = membershipService.getMembersOfGroup(groupId);
        return ResponseEntity.ok(members);
    }

    @PostMapping
    public ResponseEntity<?> addMember(@PathVariable int groupId,
                                       @RequestBody Map<String, Object> body,
                                       @RequestAttribute("userId") Integer userId) throws Exception {

        RoleGuard.requireAdminOrOwner(userId, groupId);
        Integer targetUserId = (Integer) body.get("userId");
        String role = (String) body.getOrDefault("role", "MEMBER");
        if (targetUserId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
        }

        Membership created = membershipService.addMember(groupId, targetUserId, role);
        return ResponseEntity.status(201).body(created);
    }

    @PatchMapping("/{membershipId}/role")
    public ResponseEntity<?> changeRole(@PathVariable int groupId,
                                        @PathVariable int membershipId,
                                        @RequestBody Map<String, Object> body,
                                        @RequestAttribute("userId") Integer userId) throws Exception {

        RoleGuard.requireOwner(userId, groupId);
        String role = (String) body.get("role");
        membershipService.changeRole(membershipId, role);
        return ResponseEntity.ok(Map.of("message", "Role updated"));
    }

    @DeleteMapping("/{membershipId}")
    public ResponseEntity<?> removeMember(@PathVariable int groupId,
                                          @PathVariable int membershipId,
                                          @RequestAttribute("userId") Integer userId) throws Exception {

        RoleGuard.requireMember(userId, groupId);

        Membership target = membershipService.getMembership(membershipId);
        if (target == null) {
            return ResponseEntity.notFound().build();
        }

        String actorRole = membershipService.getRole(userId, groupId);
        String targetRole = target.getRole();

        if ("OWNER".equals(targetRole) && !"OWNER".equals(actorRole)) {
            return ResponseEntity.status(403).body(Map.of("error", "Cannot remove owner"));
        }
        if ("ADMIN".equals(targetRole) && !"OWNER".equals(actorRole)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admins can only be removed by owner"));
        }

        membershipService.removeMember(membershipId);
        return ResponseEntity.noContent().build();
    }
}
