package com.app.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * Simple helper to bootstrap SQLite schema.
 */
public class Database {

    private static final String URL = "jdbc:sqlite:studyplatform.db";

    public static void init() {
        try (Connection conn = get();
             Statement st = conn.createStatement()) {

            st.execute("PRAGMA foreign_keys = ON");

            // USERS
            st.execute("""
                CREATE TABLE IF NOT EXISTS USERS (
                    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    name          TEXT    NOT NULL,
                    email         TEXT    NOT NULL UNIQUE,
                    password_hash TEXT    NOT NULL,
                    avatar_path   TEXT,
                    bio           TEXT,
                    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
                );
                """);

            ensureColumnExists(conn, "USERS", "role", "TEXT DEFAULT 'USER'");

            // GROUPS
            st.execute("""
                CREATE TABLE IF NOT EXISTS GROUPS (
                    group_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    name        TEXT    NOT NULL,
                    description TEXT,
                    created_by  INTEGER NOT NULL,
                    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
                    visibility  TEXT DEFAULT 'PRIVATE',
                    FOREIGN KEY (created_by) REFERENCES USERS(user_id) ON DELETE CASCADE
                );
                """);

            // MEMBERSHIPS
            st.execute("""
                CREATE TABLE IF NOT EXISTS MEMBERSHIPS (
                    membership_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id       INTEGER NOT NULL,
                    group_id      INTEGER NOT NULL,
                    role          TEXT    NOT NULL,
                    joined_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(user_id, group_id),
                    FOREIGN KEY (user_id)  REFERENCES USERS(user_id)  ON DELETE CASCADE,
                    FOREIGN KEY (group_id) REFERENCES GROUPS(group_id) ON DELETE CASCADE
                );
                """);

            // TASKS
            st.execute("""
                CREATE TABLE IF NOT EXISTS TASKS (
                    task_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id     INTEGER NOT NULL,
                    created_by   INTEGER NOT NULL,
                    title        TEXT    NOT NULL,
                    description  TEXT,
                    status       TEXT    NOT NULL DEFAULT 'OPEN',
                    deadline     DATETIME,
                    priority     TEXT DEFAULT 'NORMAL',
                    assigned_to  INTEGER,
                    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at   DATETIME,
                    FOREIGN KEY (group_id)    REFERENCES GROUPS(group_id) ON DELETE CASCADE,
                    FOREIGN KEY (created_by)  REFERENCES USERS(user_id)  ON DELETE SET NULL,
                    FOREIGN KEY (assigned_to) REFERENCES USERS(user_id)  ON DELETE SET NULL
                );
                """);

            // AUTO-MIGRATIONS
            ensureColumnExists(conn, "TASKS", "assigned_to", "INTEGER");
            ensureColumnExists(conn, "TASKS", "priority", "TEXT DEFAULT 'NORMAL'");

            // TASK_RESOURCES (attachments for tasks)
            st.execute("""
                CREATE TABLE IF NOT EXISTS TASK_RESOURCES (
                    task_id     INTEGER NOT NULL,
                    resource_id INTEGER NOT NULL,
                    PRIMARY KEY (task_id, resource_id),
                    FOREIGN KEY (task_id) REFERENCES TASKS(task_id) ON DELETE CASCADE,
                    FOREIGN KEY (resource_id) REFERENCES RESOURCES(resource_id) ON DELETE CASCADE
                );
            """);

            // RESOURCES
            st.execute("""
                CREATE TABLE IF NOT EXISTS RESOURCES (
                    resource_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id      INTEGER NOT NULL,
                    uploaded_by   INTEGER NOT NULL,
                    title         TEXT    NOT NULL,
                    type          TEXT    NOT NULL,
                    path_or_url   TEXT    NOT NULL,
                    original_name TEXT,
                    file_size     INTEGER,
                    description   TEXT,
                    uploaded_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (group_id)    REFERENCES GROUPS(group_id) ON DELETE CASCADE,
                    FOREIGN KEY (uploaded_by) REFERENCES USERS(user_id) ON DELETE SET NULL
                );
                """);

            ensureColumnExists(conn, "RESOURCES", "original_name", "TEXT");
            ensureColumnExists(conn, "RESOURCES", "file_size", "INTEGER");
            ensureColumnExists(conn, "RESOURCES", "description", "TEXT");

            // ACTIVITY_LOG
            st.execute("""
                CREATE TABLE IF NOT EXISTS ACTIVITY_LOG (
                    log_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id   INTEGER,
                    action    TEXT    NOT NULL,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    details   TEXT,
                    FOREIGN KEY (user_id) REFERENCES USERS(user_id) ON DELETE SET NULL
                );
                """);

            // MESSAGES
            st.execute("""
                CREATE TABLE IF NOT EXISTS MESSAGES (
                    message_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id   INTEGER,
                    user_id    INTEGER,
                    content    TEXT    NOT NULL,
                    timestamp  DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (group_id) REFERENCES GROUPS(group_id) ON DELETE CASCADE,
                    FOREIGN KEY (user_id)  REFERENCES USERS(user_id) ON DELETE SET NULL
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
                System.out.println(">>> MIGRATION: adding column " + column + " to " + table);
                st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            }
        }
    }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
