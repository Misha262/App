package com.app.repository;

import com.app.config.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TaskResourceRepository {

    public void attach(int taskId, int resourceId) throws SQLException {
        String sql = "INSERT INTO TASK_RESOURCES(task_id, resource_id) VALUES(?,?) ON CONFLICT DO NOTHING";
        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, resourceId);
            ps.executeUpdate();
        }
    }

    public void detach(int taskId, int resourceId) throws SQLException {
        String sql = "DELETE FROM TASK_RESOURCES WHERE task_id = ? AND resource_id = ?";
        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setInt(2, resourceId);
            ps.executeUpdate();
        }
    }

    public List<Integer> findResourceIdsForTask(int taskId) throws SQLException {
        String sql = "SELECT resource_id FROM TASK_RESOURCES WHERE task_id = ?";
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getInt("resource_id"));
            }
        }
        return ids;
    }
}
