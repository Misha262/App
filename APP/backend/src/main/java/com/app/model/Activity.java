package com.app.model;

/**
 * Запись в журнале активности (ACTIVITY_LOG).
 *
 * Структура таблицы ACTIVITY_LOG в БД:
 *  log_id    INTEGER PK AUTOINCREMENT
 *  user_id   INTEGER FK -> USERS
 *  action    TEXT
 *  timestamp DATETIME (по умолчанию CURRENT_TIMESTAMP)
 *  details   TEXT (JSON или просто строка)
 */
public class Activity {

    private int logId;
    private int userId;
    private String action;
    private String timestamp;
    private String details;

    public Activity(int logId, int userId, String action, String timestamp, String details) {
        this.logId = logId;
        this.userId = userId;
        this.action = action;
        this.timestamp = timestamp;
        this.details = details;
    }

    public int getLogId() {
        return logId;
    }

    public int getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getDetails() {
        return details;
    }
}
