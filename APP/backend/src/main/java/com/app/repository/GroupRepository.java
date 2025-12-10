package com.app.repository;

import com.app.config.Database;
import com.app.model.Group;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupRepository {

    /**
     * Список групп, в которых состоит пользователь.
     * Возвращаем также memberCount и openTasksCount через подзапросы.
     */
    public List<Group> findByUserId(int userId) throws SQLException {
        String sql = """
            SELECT
                g.group_id,
                g.name,
                g.description,
                g.created_by,
                g.created_at,
                (SELECT COUNT(*) FROM MEMBERSHIPS m WHERE m.group_id = g.group_id) AS member_count,
                (SELECT COUNT(*) FROM TASKS t WHERE t.group_id = g.group_id AND t.status != 'DONE') AS open_tasks
            FROM GROUPS g
            JOIN MEMBERSHIPS m2 ON m2.group_id = g.group_id
            WHERE m2.user_id = ?
            GROUP BY g.group_id, g.name, g.description, g.created_by, g.created_at
            ORDER BY g.created_at DESC;
        """;

        List<Group> result = new ArrayList<>();

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Group g = new Group(
                        rs.getInt("group_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getInt("created_by"),
                        rs.getString("created_at"),
                        rs.getInt("member_count"),
                        rs.getInt("open_tasks")
                );
                result.add(g);
            }
        }

        return result;
    }

    /**
     * Найти одну группу по ID (с теми же агрегатами, что и выше).
     */
    public Group findById(int groupId) throws SQLException {
        String sql = """
            SELECT
                g.group_id,
                g.name,
                g.description,
                g.created_by,
                g.created_at,
                (SELECT COUNT(*) FROM MEMBERSHIPS m WHERE m.group_id = g.group_id) AS member_count,
                (SELECT COUNT(*) FROM TASKS t WHERE t.group_id = g.group_id AND t.status != 'DONE') AS open_tasks
            FROM GROUPS g
            WHERE g.group_id = ?;
        """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Group(
                        rs.getInt("group_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getInt("created_by"),
                        rs.getString("created_at"),
                        rs.getInt("member_count"),
                        rs.getInt("open_tasks")
                );
            }
            return null;
        }
    }

    /**
     * Создать новую группу и сразу добавить создателя как OWNER в MEMBERSHIPS.
     * Возвращаем созданную группу с заполненными полями.
     */
    public Group createGroup(int ownerId, String name, String description) throws SQLException {
        String insertGroupSql = """
            INSERT INTO GROUPS(name, description, created_by)
            VALUES(?,?,?);
        """;

        String insertMembershipSql = """
            INSERT INTO MEMBERSHIPS(user_id, group_id, role)
            VALUES(?,?, 'OWNER');
        """;

        Connection conn = Database.get();
        try {
            conn.setAutoCommit(false);

            int groupId;

            // 1. Создаём группу
            try (PreparedStatement ps = conn.prepareStatement(
                    insertGroupSql,
                    Statement.RETURN_GENERATED_KEYS
            )) {
                ps.setString(1, name.trim());
                ps.setString(2, description == null ? "" : description.trim());
                ps.setInt(3, ownerId);

                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    groupId = keys.getInt(1);
                } else {
                    throw new SQLException("Failed to get group_id");
                }
            }

            // 2. Добавляем создателя в MEMBERSHIPS как OWNER
            try (PreparedStatement ps = conn.prepareStatement(insertMembershipSql)) {
                ps.setInt(1, ownerId);
                ps.setInt(2, groupId);
                ps.executeUpdate();
            }

            conn.commit();

            // 3. Возвращаем группу (уже из БД, с created_at и агрегатами)
            return findById(groupId);

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    /**
     * Обновить название и описание группы.
     */
    public void updateGroup(int groupId, String name, String description) throws SQLException {
        String sql = "UPDATE GROUPS SET name = ?, description = ? WHERE group_id = ?";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name.trim());
            ps.setString(2, description == null ? "" : description.trim());
            ps.setInt(3, groupId);
            ps.executeUpdate();
        }
    }

    /**
     * Удалить группу (касCADE по MEMBERSHIPS/TASKS/RESOURCES можно настроить в БД
     * или удалить вручную в сервисе).
     */
    public void deleteGroup(int groupId) throws SQLException {
        String sql = "DELETE FROM GROUPS WHERE group_id = ?";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, groupId);
            ps.executeUpdate();
        }
    }
}
