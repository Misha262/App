package com.app.repository;

import com.app.config.Database;
import com.app.model.Resource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResourceRepository {

    public List<Resource> findByGroupId(int groupId) throws SQLException {
        String sql = """
            SELECT
                r.resource_id,
                r.group_id,
                r.uploaded_by,
                r.title,
                r.type,
                r.path_or_url,
                r.original_name,
                r.file_size,
                r.description,
                r.uploaded_at,
                u.name  AS uploader_name,
                u.email AS uploader_email
            FROM RESOURCES r
            LEFT JOIN USERS u ON u.user_id = r.uploaded_by
            WHERE r.group_id = ?
            ORDER BY r.uploaded_at DESC
            """;

        List<Resource> result = new ArrayList<>();

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Resource r = mapRow(rs);
                result.add(r);
            }
        }

        return result;
    }

    public Resource findById(int resourceId) throws SQLException {
        String sql = """
            SELECT
                r.resource_id,
                r.group_id,
                r.uploaded_by,
                r.title,
                r.type,
                r.path_or_url,
                r.original_name,
                r.file_size,
                r.description,
                r.uploaded_at,
                u.name  AS uploader_name,
                u.email AS uploader_email
            FROM RESOURCES r
            LEFT JOIN USERS u ON u.user_id = r.uploaded_by
            WHERE r.resource_id = ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, resourceId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        }
    }

    public Resource createResource(int groupId,
                                   int uploadedBy,
                                   String title,
                                   String type,
                                   String pathOrUrl,
                                   String originalName,
                                   Long fileSize,
                                   String description) throws SQLException {

        String sql = """
            INSERT INTO RESOURCES(
                group_id,
                uploaded_by,
                title,
                type,
                path_or_url,
                original_name,
                file_size,
                description
            ) VALUES (?,?,?,?,?,?,?,?)
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, groupId);
            ps.setInt(2, uploadedBy);
            ps.setString(3, title);
            ps.setString(4, type);
            ps.setString(5, pathOrUrl);
            if (originalName == null) {
                ps.setNull(6, Types.VARCHAR);
            } else {
                ps.setString(6, originalName);
            }
            if (fileSize == null) {
                ps.setNull(7, Types.BIGINT);
            } else {
                ps.setLong(7, fileSize);
            }
            if (description == null) {
                ps.setNull(8, Types.VARCHAR);
            } else {
                ps.setString(8, description);
            }

            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int resourceId = keys.getInt(1);
                return findById(resourceId);
            }

            throw new SQLException("Failed to insert resource");
        }
    }

    public void deleteResource(int resourceId) throws SQLException {
        String sql = "DELETE FROM RESOURCES WHERE resource_id = ?";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, resourceId);
            ps.executeUpdate();
        }
    }

    private Resource mapRow(ResultSet rs) throws SQLException {
        return new Resource(
                rs.getInt("resource_id"),
                rs.getInt("group_id"),
                rs.getInt("uploaded_by"),
                rs.getString("title"),
                rs.getString("type"),
                rs.getString("path_or_url"),
                rs.getString("uploaded_at"),
                rs.getString("original_name"),
                rs.getObject("file_size") == null ? null : rs.getLong("file_size"),
                rs.getString("description"),
                rs.getString("uploader_name"),
                rs.getString("uploader_email")
        );
    }
}
