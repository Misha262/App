package com.app.repository;

import com.app.config.Database;
import com.app.model.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public List<Message> findRecentByGroup(int groupId, int limit) throws SQLException {
        String sql = """
            SELECT m.message_id,
                   m.group_id,
                   m.user_id,
                   m.content,
                   m.resource_id,
                   m.resource_title,
                   m.task_id,
                   m.timestamp,
                   COALESCE(u.name, 'Unknown') AS user_name
            FROM messages m
            LEFT JOIN users u ON u.user_id = m.user_id
            WHERE m.group_id = ?
            ORDER BY m.timestamp ASC
            LIMIT ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, limit);

            ResultSet rs = ps.executeQuery();
            List<Message> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Message(
                        rs.getInt("message_id"),
                        rs.getInt("group_id"),
                        rs.getInt("user_id"),
                        rs.getString("user_name"),
                        rs.getString("content"),
                        (Integer) rs.getObject("resource_id"),
                        rs.getString("resource_title"),
                        (Integer) rs.getObject("task_id"),
                        rs.getString("timestamp")
                ));
            }
            return list;
        }
    }
}
