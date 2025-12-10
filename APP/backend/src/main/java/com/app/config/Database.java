package com.app.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Simple helper to bootstrap SQLite schema.
 */
public class Database {

    private static final String URL = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/appdb");
    private static final String USER = System.getenv().getOrDefault("DB_USER", "appuser");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");

    public static void init() {
        try (Connection conn = get();
             Statement st = conn.createStatement()) {

            // USERS
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    user_id       SERIAL PRIMARY KEY,
                    name          TEXT    NOT NULL,
                    email         TEXT    NOT NULL UNIQUE,
                    password_hash TEXT    NOT NULL,
                    avatar_path   TEXT,
                    bio           TEXT,
                    role          TEXT    DEFAULT 'USER',
                    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """);

            // GROUPS
            st.execute("""
                CREATE TABLE IF NOT EXISTS groups (
                    group_id    SERIAL PRIMARY KEY,
                    name        TEXT    NOT NULL,
                    description TEXT,
                    created_by  INTEGER NOT NULL,
                    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    visibility  TEXT DEFAULT 'PRIVATE',
                    FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE CASCADE
                );
                """);

            // MEMBERSHIPS
            st.execute("""
                CREATE TABLE IF NOT EXISTS memberships (
                    membership_id SERIAL PRIMARY KEY,
                    user_id       INTEGER NOT NULL,
                    group_id      INTEGER NOT NULL,
                    role          TEXT    NOT NULL,
                    joined_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(user_id, group_id),
                    FOREIGN KEY (user_id)  REFERENCES users(user_id)  ON DELETE CASCADE,
                    FOREIGN KEY (group_id) REFERENCES groups(group_id) ON DELETE CASCADE
                );
                """);

            // TASKS
            st.execute("""
                CREATE TABLE IF NOT EXISTS tasks (
                    task_id      SERIAL PRIMARY KEY,
                    group_id     INTEGER NOT NULL,
                    created_by   INTEGER NOT NULL,
                    title        TEXT    NOT NULL,
                    description  TEXT,
                    status       TEXT    NOT NULL DEFAULT 'OPEN',
                    deadline     TIMESTAMP,
                    priority     TEXT DEFAULT 'NORMAL',
                    assigned_to  INTEGER,
                    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at   TIMESTAMP,
                    FOREIGN KEY (group_id)    REFERENCES groups(group_id) ON DELETE CASCADE,
                    FOREIGN KEY (created_by)  REFERENCES users(user_id)  ON DELETE SET NULL,
                    FOREIGN KEY (assigned_to) REFERENCES users(user_id)  ON DELETE SET NULL
                );
                """);

            // AUTO-MIGRATIONS (kept for compatibility)
            ensureColumnExists(conn, "tasks", "assigned_to", "INTEGER");
            ensureColumnExists(conn, "tasks", "priority", "TEXT DEFAULT 'NORMAL'");

            // RESOURCES (создаём до task_resources, т.к. есть FK)
            st.execute("""
                CREATE TABLE IF NOT EXISTS resources (
                    resource_id   SERIAL PRIMARY KEY,
                    group_id      INTEGER NOT NULL,
                    uploaded_by   INTEGER NOT NULL,
                    title         TEXT    NOT NULL,
                    type          TEXT    NOT NULL,
                    path_or_url   TEXT    NOT NULL,
                    original_name TEXT,
                    file_size     BIGINT,
                    description   TEXT,
                    uploaded_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (group_id)    REFERENCES groups(group_id) ON DELETE CASCADE,
                    FOREIGN KEY (uploaded_by) REFERENCES users(user_id) ON DELETE SET NULL
                );
                """);

            ensureColumnExists(conn, "resources", "original_name", "TEXT");
            ensureColumnExists(conn, "resources", "file_size", "BIGINT");
            ensureColumnExists(conn, "resources", "description", "TEXT");

            // TASK_RESOURCES (attachments for tasks)
            st.execute("""
                CREATE TABLE IF NOT EXISTS task_resources (
                    task_id     INTEGER NOT NULL,
                    resource_id INTEGER NOT NULL,
                    PRIMARY KEY (task_id, resource_id),
                    FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE,
                    FOREIGN KEY (resource_id) REFERENCES resources(resource_id) ON DELETE CASCADE
                );
            """);

            // ACTIVITY_LOG
            st.execute("""
                CREATE TABLE IF NOT EXISTS activity_log (
                    log_id    SERIAL PRIMARY KEY,
                    user_id   INTEGER,
                    action    TEXT    NOT NULL,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    details   TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
                );
                """);

            // MESSAGES
            st.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    message_id SERIAL PRIMARY KEY,
                    group_id   INTEGER,
                    user_id    INTEGER,
                    content    TEXT    NOT NULL,
                    timestamp  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (group_id) REFERENCES groups(group_id) ON DELETE CASCADE,
                    FOREIGN KEY (user_id)  REFERENCES users(user_id) ON DELETE SET NULL
                );
                """);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to init database", e);
        }
    }

    private static void ensureColumnExists(Connection conn, String table, String column, String type)
            throws SQLException {

        boolean exists = false;
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, table, column)) {
            if (rs.next()) exists = true;
        }

        if (!exists) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            }
        }
    }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
