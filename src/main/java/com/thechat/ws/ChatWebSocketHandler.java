package com.thechat.ws;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.thechat.conversation.ConversationNotFoundException;
import com.thechat.message.MessageService;
import com.thechat.ws.dto.SendMessagePayload;
import com.thechat.ws.dto.WsEnvelope;
import com.thechat.ws.dto.WsErrorPayload;
import com.thechat.ws.dto.WsReadyPayload;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final JsonMapper jsonMapper;
    private final WsConnectionRegistry connectionRegistry;
    private final MessageService messageService;

    public ChatWebSocketHandler(
            JsonMapper jsonMapper,
            WsConnectionRegistry connectionRegistry,
            MessageService messageService) {
        this.jsonMapper = jsonMapper;
        this.connectionRegistry = connectionRegistry;
        this.messageService = messageService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID userId = requireUserId(session);
        connectionRegistry.register(userId, session);
        sendEvent(session, WsEventTypes.READY, new WsReadyPayload(userId));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        UUID userId = requireUserId(session);
        WsEnvelope envelope;
        try {
            envelope = jsonMapper.readValue(message.getPayload(), WsEnvelope.class);
        } catch (JacksonException ex) {
            sendError(session, "invalid_payload", "Invalid WebSocket payload", null);
            return;
        }

        if (envelope.type() == null || envelope.type().isBlank()) {
            sendError(session, "invalid_type", "Event type is required", null);
            return;
        }

        switch (envelope.type()) {
            case WsEventTypes.PING -> sendEvent(session, WsEventTypes.PONG, Map.of());
            case WsEventTypes.MESSAGE_SEND -> handleMessageSend(session, userId, envelope.payload());
            default -> sendError(session, "unknown_type", "Unknown event type: " + envelope.type(), null);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID userId = (UUID) session.getAttributes().get(JwtHandshakeInterceptor.USER_ID_ATTR);
        if (userId != null) {
            connectionRegistry.unregister(userId, session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error for session {}", session.getId(), exception);
        UUID userId = (UUID) session.getAttributes().get(JwtHandshakeInterceptor.USER_ID_ATTR);
        if (userId != null) {
            connectionRegistry.unregister(userId, session);
        }
    }

    private void handleMessageSend(WebSocketSession session, UUID userId, JsonNode payloadNode) {
        if (payloadNode == null || payloadNode.isNull()) {
            sendError(session, "invalid_payload", "Invalid message_send payload", null);
            return;
        }

        SendMessagePayload payload;
        try {
            payload = jsonMapper.treeToValue(payloadNode, SendMessagePayload.class);
        } catch (JacksonException | IllegalArgumentException ex) {
            sendError(session, "invalid_payload", "Invalid message_send payload", null);
            return;
        }

        if (payload == null || payload.conversationId() == null) {
            sendError(session, "invalid_payload", "conversationId is required", null);
            return;
        }

        try {
            messageService.acceptAndBroadcast(userId, payload.conversationId(), payload.content());
        } catch (IllegalArgumentException ex) {
            sendError(session, "validation_error", ex.getMessage(), payload.conversationId());
        } catch (ConversationNotFoundException ex) {
            sendError(session, "forbidden", "Not a participant of this conversation", payload.conversationId());
        } catch (RuntimeException ex) {
            log.error("Failed to accept message from user {}", userId, ex);
            sendError(session, "server_error", "Failed to send message", payload.conversationId());
        }
    }

    private UUID requireUserId(WebSocketSession session) {
        Object raw = session.getAttributes().get(JwtHandshakeInterceptor.USER_ID_ATTR);
        if (!(raw instanceof UUID userId)) {
            throw new IllegalStateException("WebSocket session is missing authenticated userId");
        }
        return userId;
    }

    private void sendError(WebSocketSession session, String code, String message, UUID conversationId) {
        sendEvent(session, WsEventTypes.ERROR, new WsErrorPayload(code, message, conversationId));
    }

    private void sendEvent(WebSocketSession session, String type, Object payload) {
        String json = jsonMapper.writeValueAsString(new WsEnvelope(type, jsonMapper.valueToTree(payload)));
        connectionRegistry.send(session, json);
    }
}
