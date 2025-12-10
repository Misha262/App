package com.app.service;

import com.app.model.Membership;
import com.app.repository.MembershipRepository;

import java.sql.SQLException;
import java.util.List;

public class MembershipService {

    private final MembershipRepository membershipRepo = new MembershipRepository();

    // ============================================================
    // 📌 БАЗОВЫЕ ОПЕРАЦИИ MEMBERSHIP
    // ============================================================

    /** Получить всех участников группы. */
    public List<Membership> getMembersOfGroup(int groupId) throws SQLException {
        return membershipRepo.findByGroupId(groupId);
    }

    /** Добавить участника в группу. */
    public Membership addMember(int groupId, int userId, String role) throws Exception {
        if (groupId <= 0 || userId <= 0) {
            throw new IllegalArgumentException("groupId and userId are required");
        }
        if (role == null || role.isBlank()) {
            role = "MEMBER";
        }

        role = role.toUpperCase();
        if (!role.equals("OWNER") && !role.equals("ADMIN") && !role.equals("MEMBER")) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        return membershipRepo.createMembership(userId, groupId, role);
    }

    /** Изменить роль участника. */
    public void changeRole(int membershipId, String role) throws Exception {
        if (membershipId <= 0) {
            throw new IllegalArgumentException("membershipId is required");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role is required");
        }

        role = role.toUpperCase();
        if (!role.equals("OWNER") && !role.equals("ADMIN") && !role.equals("MEMBER")) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        membershipRepo.updateRole(membershipId, role);
    }

    /** Удалить участника из группы. */
    public void removeMember(int membershipId) throws SQLException {
        membershipRepo.deleteMembership(membershipId);
    }

    /** Получить membership по membershipId. */
    public Membership getMembership(int membershipId) throws SQLException {
        return membershipRepo.findById(membershipId);
    }


    // ============================================================
    // 🔥 МЕТОДЫ ДЛЯ SECURITY / ROLEGUARD
    // ============================================================

    /** Получить членство userId → groupId. */
    public Membership getMembershipByUserAndGroup(int userId, int groupId) throws SQLException {
        return membershipRepo.findByUserAndGroup(userId, groupId);
    }

    /** Проверить, является ли пользователь участником группы. */
    public boolean isMember(int userId, int groupId) throws SQLException {
        return membershipRepo.findByUserAndGroup(userId, groupId) != null;
    }

    /** Получить роль участника: OWNER / ADMIN / MEMBER / null. */
    public String getRole(int userId, int groupId) throws SQLException {
        Membership m = membershipRepo.findByUserAndGroup(userId, groupId);
        return (m != null) ? m.getRole() : null;
    }

    /** Проверить, является ли пользователь OWNER. */
    public boolean isOwner(int userId, int groupId) throws SQLException {
        return "OWNER".equals(getRole(userId, groupId));
    }

    /** Проверить, является ли пользователь ADMIN или OWNER. */
    public boolean isAdminOrOwner(int userId, int groupId) throws SQLException {
        String role = getRole(userId, groupId);
        return role != null && (role.equals("ADMIN") || role.equals("OWNER"));
    }
}
