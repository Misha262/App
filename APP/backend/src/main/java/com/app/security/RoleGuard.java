package com.app.security;

import com.app.model.Membership;
import com.app.service.MembershipService;

public class RoleGuard {

    private static final MembershipService membershipService = new MembershipService();

    // =====================================================================
    // BASIC CHECKS — возвращают boolean и НЕ бросают checked exception
    // =====================================================================

    /** Проверка: пользователь состоит в группе? */
    public static boolean isMember(int userId, int groupId) {
        try {
            Membership m = membershipService.getMembershipByUserAndGroup(userId, groupId);
            return m != null;
        } catch (Exception e) {
            return false;
        }
    }

    /** Получить роль или null */
    public static String getRole(int userId, int groupId) {
        try {
            Membership m = membershipService.getMembershipByUserAndGroup(userId, groupId);
            return (m == null) ? null : m.getRole();
        } catch (Exception e) {
            return null;
        }
    }

    /** Проверка OWNER */
    public static boolean isOwner(int userId, int groupId) {
        return "OWNER".equals(getRole(userId, groupId));
    }

    /** Проверка ADMIN или OWNER */
    public static boolean isAdminOrOwner(int userId, int groupId) {
        String role = getRole(userId, groupId);
        return role != null && (role.equals("ADMIN") || role.equals("OWNER"));
    }

    // =====================================================================
    // HARD GUARDS — бросают SecurityException (но НЕ checked)
    // =====================================================================

    /** Требует MEMBER */
    public static void requireMember(int userId, int groupId) {
        if (!isMember(userId, groupId)) {
            throw new SecurityException("Access denied: only group members allowed");
        }
    }

    /** Требует ADMIN или OWNER */
    public static void requireAdminOrOwner(int userId, int groupId) {
        if (!isAdminOrOwner(userId, groupId)) {
            throw new SecurityException("Access denied: only ADMIN/OWNER allowed");
        }
    }

    /** Требует OWNER */
    public static void requireOwner(int userId, int groupId) {
        if (!isOwner(userId, groupId)) {
            throw new SecurityException("Access denied: only OWNER allowed");
        }
    }
}
