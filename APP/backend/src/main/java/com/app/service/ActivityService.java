package com.app.service;

import com.app.model.Activity;
import com.app.repository.ActivityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Сервис журналирования активности.
 *
 * Удобный метод:
 *  log(userId, "TASK_CREATED", Map.of("groupId", gId, "taskId", tId))
 */
public class ActivityService {

    private final ActivityRepository repo = new ActivityRepository();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Записать в лог с произвольной строкой details.
     */
    public void log(int userId, String action, String details) throws SQLException {
        repo.insert(userId, action, details);
    }

    /**
     * Записать в лог, сериализуя карту в JSON.
     */
    public void log(int userId, String action, Map<String, Object> details) throws SQLException {
        try {
            String json = mapper.writeValueAsString(details);
            repo.insert(userId, action, json);
        } catch (Exception e) {
            // Если по какой-то причине JSON не сериализовался — пишем как toString()
            repo.insert(userId, action, String.valueOf(details));
        }
    }

    public List<Activity> recentForUser(int userId, int limit) throws SQLException {
        return repo.findRecentForUser(userId, limit);
    }

    public List<Activity> recentForGroup(int groupId, int limit) throws SQLException {
        return repo.findRecentForGroup(groupId, limit);
    }
}
