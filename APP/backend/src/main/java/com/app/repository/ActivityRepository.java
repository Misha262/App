package com.app.repository;

import com.app.config.Database;
import com.app.model.Activity;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Доступ к таблице ACTIVITY_LOG.
 */
public class ActivityRepository {

    /**
     * Записать новую активность.
     *
     * timestamp задаём через datetime('now') на стороне SQLite.
     */
    public void insert(int userId, String action, String details) throws SQLException {
        String sql = """
            INSERT INTO ACTIVITY_LOG (user_id, action, details, timestamp)
            VALUES (?, ?, ?, datetime('now'))
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, details);
            ps.executeUpdate();
        }
    }

    /**
     * Последние N записей по конкретному пользователю.
     */
    public List<Activity> findRecentForUser(int userId, int limit) throws SQLException {
        String sql = """
            SELECT log_id, user_id, action, timestamp, details
            FROM ACTIVITY_LOG
            WHERE user_id = ?
            ORDER BY timestamp DESC
            LIMIT ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, limit);

            ResultSet rs = ps.executeQuery();
            List<Activity> list = new ArrayList<>();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

            return list;
        }
    }

    /**
     * Последние N записей по группе.
     *
     * Для простоты фильтруем по подстроке в JSON (details).
     * Ожидаем, что в details будет поле "groupId": <id>.
     */
    public List<Activity> findRecentForGroup(int groupId, int limit) throws SQLException {
        String sql = """
            SELECT log_id, user_id, action, timestamp, details
            FROM ACTIVITY_LOG
            WHERE details LIKE ?
            ORDER BY timestamp DESC
            LIMIT ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // ищем подстроку "groupId":<id> внутри JSON-строки
            String pattern = "%\"groupId\":" + groupId + "%";
            ps.setString(1, pattern);
            ps.setInt(2, limit);

            ResultSet rs = ps.executeQuery();
            List<Activity> list = new ArrayList<>();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

            return list;
        }
    }

    private Activity mapRow(ResultSet rs) throws SQLException {
        return new Activity(
                rs.getInt("log_id"),
                rs.getInt("user_id"),
                rs.getString("action"),
                rs.getString("timestamp"),
                rs.getString("details")
        );
    }
}
