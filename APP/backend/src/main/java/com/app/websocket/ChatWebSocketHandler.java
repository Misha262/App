package com.app.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.app.config.Database;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static class SessionInfo {
        final int userId;
        final String userName;
        final Set<Integer> groupIds;

        SessionInfo(int userId, String userName, Set<Integer> groupIds) {
            this.userId = userId;
            this.userName = userName;
            this.groupIds = groupIds;
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<WebSocketSession, SessionInfo> sessionInfo = new ConcurrentHashMap<>();
    private final Map<Integer, Set<WebSocketSession>> groupSessions = new ConcurrentHashMap<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode node = mapper.readTree(message.getPayload());
        String type = node.path("type").asText("");

        if ("join".equalsIgnoreCase(type) || "joinMultiple".equalsIgnoreCase(type)) {
            int userId = node.path("userId").asInt();
            String userName = node.path("userName").asText("");
            Set<Integer> groups = ConcurrentHashMap.newKeySet();

            if ("joinMultiple".equalsIgnoreCase(type)) {
                node.path("groupIds").forEach(g -> groups.add(g.asInt()));
            } else {
                groups.add(node.path("groupId").asInt());
            }

            SessionInfo info = new SessionInfo(userId, userName, groups);
            sessionInfo.put(session, info);

            for (Integer gid : groups) {
                groupSessions.computeIfAbsent(gid, k -> ConcurrentHashMap.newKeySet()).add(session);
                broadcastOnline(gid);
            }
            return;
        }

        SessionInfo info = sessionInfo.get(session);
        if (info == null) {
            return;
        }

        int targetGroupId = node.path("groupId").asInt();
        if (targetGroupId == 0 && !info.groupIds.isEmpty()) {
            targetGroupId = info.groupIds.iterator().next();
        }

        if ("message".equalsIgnoreCase(type)) {
            ObjectNode payload = mapper.createObjectNode();
            payload.put("type", "message");
            payload.put("groupId", targetGroupId);
            payload.put("userId", node.path("userId").asInt(info.userId));
            payload.put("userName", node.path("userName").asText(info.userName));
            payload.put("text", node.path("text").asText(""));
            payload.put("timestamp", node.has("timestamp") ? node.path("timestamp").asText() : Instant.now().toString());
            if (node.has("resourceId")) {
                payload.put("resourceId", node.path("resourceId").asInt());
                payload.put("resourceTitle", node.path("resourceTitle").asText(""));
            }
            if (node.has("taskId")) {
                payload.put("taskId", node.path("taskId").asInt());
            }

            // Persist message to DB; failures should not crash websocket
            Integer resId = node.has("resourceId") ? node.path("resourceId").asInt() : null;
            String resTitle = node.has("resourceTitle") ? node.path("resourceTitle").asText("") : null;
            Integer taskId = node.has("taskId") ? node.path("taskId").asInt() : null;

            saveMessage(
                    targetGroupId,
                    node.path("userId").asInt(info.userId),
                    node.path("text").asText(""),
                    resId,
                    resTitle,
                    taskId
            );

            broadcast(targetGroupId, payload);
        } else if ("typing".equalsIgnoreCase(type)) {
            ObjectNode payload = mapper.createObjectNode();
            payload.put("type", "typing");
            payload.put("groupId", targetGroupId);
            payload.put("userId", node.path("userId").asInt(info.userId));
            payload.put("userName", node.path("userName").asText(info.userName));

            broadcast(targetGroupId, payload);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SessionInfo info = sessionInfo.remove(session);
        if (info != null) {
            for (Integer gid : info.groupIds) {
                Set<WebSocketSession> sessions = groupSessions.get(gid);
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        groupSessions.remove(gid);
                    } else {
                        broadcastOnline(gid);
                    }
                }
            }
        }
        super.afterConnectionClosed(session, status);
    }

    private void broadcast(int groupId, ObjectNode payload) {
        Set<WebSocketSession> sessions = groupSessions.get(groupId);
        if (sessions == null || sessions.isEmpty()) return;

        TextMessage msg = new TextMessage(payload.toString());
        sessions.forEach(s -> {
            if (s.isOpen()) {
                try {
                    s.sendMessage(msg);
                } catch (Exception ignored) {
                }
            }
        });
    }

    public void broadcastEvent(int groupId, Map<String, Object> payload) {
        ObjectNode node = mapper.createObjectNode();
        payload.forEach((k, v) -> node.set(k, mapper.valueToTree(v)));
        broadcast(groupId, node);
    }

    private void broadcastOnline(int groupId) {
        Set<WebSocketSession> sessions = groupSessions.get(groupId);
        if (sessions == null) return;

        // Deduplicate by userId so один пользователь в нескольких сессиях не удваивает онлайн
        List<String> users = sessions.stream()
                .map(sessionInfo::get)
                .filter(info -> info != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(i -> i.userId, i -> i.userName, (a, b) -> a),
                        m -> m.values().stream().collect(Collectors.toList())
                ));

        ObjectNode payload = mapper.createObjectNode();
        payload.put("type", "online");
        payload.put("groupId", groupId);
        payload.putPOJO("users", users);

        broadcast(groupId, payload);
    }

    private void saveMessage(int groupId, int userId, String text, Integer resourceId, String resourceTitle, Integer taskId) {
        String sql = "INSERT INTO messages(group_id, user_id, content, resource_id, resource_title, task_id, timestamp) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            ps.setString(3, text);
            if (resourceId == null || resourceId == 0) ps.setNull(4, java.sql.Types.INTEGER); else ps.setInt(4, resourceId);
            if (resourceTitle == null) ps.setNull(5, java.sql.Types.VARCHAR); else ps.setString(5, resourceTitle);
            if (taskId == null || taskId == 0) ps.setNull(6, java.sql.Types.INTEGER); else ps.setInt(6, taskId);
            ps.executeUpdate();
        } catch (Exception ignored) {
            // intentionally ignore to avoid breaking websocket flow
        }
    }
}
