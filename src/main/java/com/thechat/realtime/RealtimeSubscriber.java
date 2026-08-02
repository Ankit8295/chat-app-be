package com.thechat.realtime;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.thechat.AppProperties;
import com.thechat.ws.WsConnectionRegistry;
import com.thechat.ws.WsEventTypes;

import tools.jackson.databind.json.JsonMapper;

@Component
public class RealtimeSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RealtimeSubscriber.class);

    private final JsonMapper jsonMapper;
    private final WsConnectionRegistry connectionRegistry;
    private final String instanceId;

    public RealtimeSubscriber(
            JsonMapper jsonMapper,
            WsConnectionRegistry connectionRegistry,
            AppProperties appProperties) {
        this.jsonMapper = jsonMapper;
        this.connectionRegistry = connectionRegistry;
        this.instanceId = appProperties.id();
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String json = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            RealtimeEvent event = jsonMapper.readValue(json, RealtimeEvent.class);
            if (event.type() == null || event.type().isBlank()) {
                log.warn("[instance-{}] Ignoring realtime event with missing type", instanceId);
                return;
            }
            if (event.targetUserIds() == null || event.envelope() == null) {
                log.warn("[instance-{}] Ignoring realtime event with missing targets/envelope", instanceId);
                return;
            }

            switch (event.type()) {
                case WsEventTypes.MESSAGE_NEW,
                        WsEventTypes.MESSAGE_UPDATED,
                        WsEventTypes.MESSAGE_DELETED ->
                    sendMessageEvent(event);
                case WsEventTypes.MEMBER_ADDED,
                        WsEventTypes.MEMBER_REMOVED,
                        WsEventTypes.GROUP_UPDATED ->
                    sendGroupEvent(event);
                default -> log.warn(
                        "[instance-{}] Unknown realtime type={} eventId={}",
                        instanceId,
                        event.type(),
                        event.eventId());
            }
        } catch (Exception ex) {
            log.error("[instance-{}] Failed to handle realtime message: {}", instanceId, json, ex);
        }
    }

    private void sendMessageEvent(RealtimeEvent event) {
        deliverToLocalUsers(event);
        log.info(
                "[instance-{}] Message event delivered type={} eventId={} fromOrigin={} targetCount={}",
                instanceId,
                event.type(),
                event.eventId(),
                event.originInstanceId(),
                event.targetUserIds().size());
    }

    private void sendGroupEvent(RealtimeEvent event) {
        deliverToLocalUsers(event);
        log.info(
                "[instance-{}] Group event delivered type={} eventId={} fromOrigin={} targetCount={}",
                instanceId,
                event.type(),
                event.eventId(),
                event.originInstanceId(),
                event.targetUserIds().size());
    }

    private void deliverToLocalUsers(RealtimeEvent event) {
        String envelopeJson = jsonMapper.writeValueAsString(event.envelope());
        for (UUID userId : event.targetUserIds()) {
            if (userId == null) {
                continue;
            }
            connectionRegistry.sendToUser(userId, envelopeJson);
        }
    }
}
