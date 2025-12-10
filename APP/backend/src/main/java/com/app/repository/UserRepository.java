package com.app.repository;

import com.app.config.Database;
import com.app.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Low level access to USERS table.
 */
public class UserRepository {

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM USERS WHERE email = ?";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public User createUser(String name, String email, String passwordHash) throws SQLException {
        String sql = """
            INSERT INTO USERS(name, email, password_hash, role)
            VALUES(?,?,?,?)
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, "USER");
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return findById(keys.getInt(1));
            }
            throw new SQLException("Failed to insert user");
        }
    }

    public User findById(int id) throws SQLException {
        String sql = """
            SELECT user_id, name, email, avatar_path, bio, created_at, password_hash, role
            FROM USERS
            WHERE user_id = ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        }
    }

    public User findByEmail(String email) throws SQLException {
        String sql = """
            SELECT user_id, name, email, avatar_path, bio, created_at, password_hash, role
            FROM USERS
            WHERE email = ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        }
    }

    public String getPasswordHashByEmail(String email) throws SQLException {
        String sql = "SELECT password_hash FROM USERS WHERE email = ?";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("password_hash");
            }
            return null;
        }
    }

    public String getPasswordHashById(int userId) throws SQLException {
        String sql = "SELECT password_hash FROM USERS WHERE user_id = ?";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("password_hash");
            }
            return null;
        }
    }

    public void updatePasswordHash(int userId, String passwordHash) throws SQLException {
        String sql = "UPDATE USERS SET password_hash = ? WHERE user_id = ?";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public User updateProfile(int userId, String name, String bio, String avatarPath) throws SQLException {
        String sql = "UPDATE USERS SET name = ?, bio = ?, avatar_path = ? WHERE user_id = ?";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, bio);
            ps.setString(3, avatarPath);
            ps.setInt(4, userId);
            ps.executeUpdate();
        }

        return findById(userId);
    }

    public void updateEmail(int userId, String email) throws SQLException {
        String sql = "UPDATE USERS SET email = ? WHERE user_id = ?";
        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        if (role == null || role.isBlank()) {
            role = "USER";
        }
        return new User(
                rs.getInt("user_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("avatar_path"),
                rs.getString("bio"),
                rs.getString("created_at"),
                rs.getString("password_hash"),
                role
        );
    }
}
