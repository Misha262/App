package com.app.repository;

import com.app.config.Database;
import com.app.model.Task;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskRepository {

    /**
     * Список задач по группе.
     */
    public List<Task> findByGroupId(int groupId) throws SQLException {
        String sql = """
            SELECT
                task_id,
                group_id,
                created_by,
                assigned_to,
                title,
                description,
                status,
                deadline,
                priority,
                created_at,
                updated_at
            FROM TASKS
            WHERE group_id = ?
            ORDER BY
                deadline IS NULL,   -- сначала с дедлайном, потом без
                deadline ASC,
                created_at DESC
            """;

        List<Task> result = new ArrayList<>();

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Task t = new Task(
                        rs.getInt("task_id"),
                        rs.getInt("group_id"),
                        rs.getInt("created_by"),
                        (Integer) rs.getObject("assigned_to"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getString("deadline"),
                        rs.getString("priority"),
                        rs.getString("created_at"),
                        rs.getString("updated_at")
                );
                result.add(t);
            }
        }

        return result;
    }

    /**
     * Найти одну задачу по её ID.
     */
    public Task findById(int taskId) throws SQLException {
        String sql = """
            SELECT
                task_id,
                group_id,
                created_by,
                assigned_to,
                title,
                description,
                status,
                deadline,
                priority,
                created_at,
                updated_at
            FROM TASKS
            WHERE task_id = ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Task(
                        rs.getInt("task_id"),
                        rs.getInt("group_id"),
                        rs.getInt("created_by"),
                        (Integer) rs.getObject("assigned_to"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getString("deadline"),
                        rs.getString("priority"),
                        rs.getString("created_at"),
                        rs.getString("updated_at")
                );
            }
            return null;
        }
    }

    /**
     * Создать новую задачу в группе.
     */
    public Task createTask(int groupId,
                           int createdBy,
                           Integer assignedTo,
                           String title,
                           String description,
                           String status,
                           String deadline,
                           String priority) throws SQLException {

        String sql = """
            INSERT INTO TASKS(
                group_id,
                created_by,
                assigned_to,
                title,
                description,
                status,
                deadline,
                priority
            ) VALUES (?,?,?,?,?,?,?,?)
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, groupId);
            ps.setInt(2, createdBy);

            if (assignedTo == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, assignedTo);
            }

            ps.setString(4, title);
            ps.setString(5, description);
            ps.setString(6, status);

            if (deadline == null || deadline.isBlank()) {
                ps.setNull(7, Types.VARCHAR);
            } else {
                ps.setString(7, deadline);
            }

            ps.setString(8, priority);

            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int taskId = keys.getInt(1);
                return findById(taskId);
            }

            throw new SQLException("Failed to insert task");
        }
    }

    /**
     * Обновить поля задачи (кроме статуса).
     */
    public void updateTask(int taskId,
                           Integer assignedTo,
                           String title,
                           String description,
                           String deadline,
                           String priority) throws SQLException {

        String sql = """
            UPDATE TASKS
            SET
                assigned_to = ?,
                title = ?,
                description = ?,
                deadline = ?,
                priority = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE task_id = ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (assignedTo == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, assignedTo);
            }

            ps.setString(2, title);
            ps.setString(3, description);

            if (deadline == null || deadline.isBlank()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, deadline);
            }

            ps.setString(5, priority);
            ps.setInt(6, taskId);

            ps.executeUpdate();
        }
    }

    /**
     * Обновить только статус задачи.
     */
    public void updateStatus(int taskId, String status) throws SQLException {
        String sql = """
            UPDATE TASKS
            SET status = ?, updated_at = CURRENT_TIMESTAMP
            WHERE task_id = ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, taskId);
            ps.executeUpdate();
        }
    }

    /**
     * Удалить задачу.
     */
    public void deleteTask(int taskId) throws SQLException {
        String sql = "DELETE FROM TASKS WHERE task_id = ?";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.executeUpdate();
        }
    }
}
