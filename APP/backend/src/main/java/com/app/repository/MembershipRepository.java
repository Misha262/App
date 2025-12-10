package com.app.repository;

import com.app.config.Database;
import com.app.model.Membership;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MembershipRepository {

    /**
     * Все участники группы с данными пользователя (имя + email).
     */
    public List<Membership> findByGroupId(int groupId) throws SQLException {
        String sql = """
            SELECT
                m.membership_id,
                m.user_id,
                m.group_id,
                m.role,
                m.joined_at,
                u.name AS user_name,
                u.email AS user_email
            FROM MEMBERSHIPS m
            JOIN USERS u ON u.user_id = m.user_id
            WHERE m.group_id = ?
            ORDER BY m.joined_at ASC
            """;

        List<Membership> result = new ArrayList<>();

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Membership m = new Membership(
                        rs.getInt("membership_id"),
                        rs.getInt("user_id"),
                        rs.getInt("group_id"),
                        rs.getString("role"),
                        rs.getString("joined_at"),
                        rs.getString("user_name"),
                        rs.getString("user_email")
                );
                result.add(m);
            }
        }

        return result;
    }

    public Membership findById(int membershipId) throws SQLException {
        String sql = """
            SELECT
                m.membership_id,
                m.user_id,
                m.group_id,
                m.role,
                m.joined_at,
                u.name AS user_name,
                u.email AS user_email
            FROM MEMBERSHIPS m
            JOIN USERS u ON u.user_id = m.user_id
            WHERE m.membership_id = ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, membershipId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Membership(
                        rs.getInt("membership_id"),
                        rs.getInt("user_id"),
                        rs.getInt("group_id"),
                        rs.getString("role"),
                        rs.getString("joined_at"),
                        rs.getString("user_name"),
                        rs.getString("user_email")
                );
            }
            return null;
        }
    }

    /**
     * Найти членство по userId + groupId (полезно для проверок).
     */
    public Membership findByUserAndGroup(int userId, int groupId) throws SQLException {
        String sql = """
            SELECT
                m.membership_id,
                m.user_id,
                m.group_id,
                m.role,
                m.joined_at,
                u.name AS user_name,
                u.email AS user_email
            FROM MEMBERSHIPS m
            JOIN USERS u ON u.user_id = m.user_id
            WHERE m.user_id = ? AND m.group_id = ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, groupId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Membership(
                        rs.getInt("membership_id"),
                        rs.getInt("user_id"),
                        rs.getInt("group_id"),
                        rs.getString("role"),
                        rs.getString("joined_at"),
                        rs.getString("user_name"),
                        rs.getString("user_email")
                );
            }
            return null;
        }
    }

    /**
     * Добавить участника в группу.
     */
    public Membership createMembership(int userId, int groupId, String role) throws SQLException {
        String sql = """
            INSERT INTO MEMBERSHIPS(user_id, group_id, role)
            VALUES(?,?,?)
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, userId);
            ps.setInt(2, groupId);
            ps.setString(3, role);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int membershipId = keys.getInt(1);
                // Возвращаем из БД, чтобы заполнить joined_at + имя/email
                return findById(membershipId);
            }
            throw new SQLException("Failed to insert membership");
        }
    }

    /**
     * Обновить роль участника.
     */
    public void updateRole(int membershipId, String role) throws SQLException {
        String sql = "UPDATE MEMBERSHIPS SET role = ? WHERE membership_id = ?";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role);
            ps.setInt(2, membershipId);
            ps.executeUpdate();
        }
    }

    /**
     * Удалить участника из группы.
     */
    public void deleteMembership(int membershipId) throws SQLException {
        String sql = "DELETE FROM MEMBERSHIPS WHERE membership_id = ?";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, membershipId);
            ps.executeUpdate();
        }
    }
}
