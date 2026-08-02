package com.thechat.ws;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class WsConnectionRegistry {

    private static final Logger log = LoggerFactory.getLogger(WsConnectionRegistry.class);

    private final Map<UUID, Set<WebSocketSession>> sessionsByUserId = new ConcurrentHashMap<>();

    public void register(UUID userId, WebSocketSession session) {
        sessionsByUserId
                .computeIfAbsent(userId, ignored -> new CopyOnWriteArraySet<>())
                .add(session);
    }

    public void unregister(UUID userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByUserId.remove(userId, sessions);
        }
    }

    public void sendToUser(UUID userId, String json) {
        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            send(session, message);
        }
    }

    public void sendToUsers(Iterable<UUID> userIds, String json) {
        TextMessage message = new TextMessage(json);
        for (UUID userId : userIds) {
            Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
            if (sessions == null || sessions.isEmpty()) {
                continue;
            }
            for (WebSocketSession session : sessions) {
                send(session, message);
            }
        }
    }

    public void send(WebSocketSession session, String json) {
        send(session, new TextMessage(json));
    }

    private void send(WebSocketSession session, TextMessage message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(message);
            }
        } catch (IOException ex) {
            log.warn("Failed to send WebSocket message to session {}", session.getId(), ex);
        }
    }
}
