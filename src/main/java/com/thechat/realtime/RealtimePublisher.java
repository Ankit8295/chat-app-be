package com.thechat.realtime;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.thechat.AppProperties;
import com.thechat.message.dto.MessageResponse;
import com.thechat.ws.WsEventTypes;
import com.thechat.ws.dto.WsEnvelope;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class RealtimePublisher {

    private static final Logger log = LoggerFactory.getLogger(RealtimePublisher.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final JsonMapper jsonMapper;
    private final String channel;
    private final String instanceId;

    public RealtimePublisher(
            StringRedisTemplate stringRedisTemplate,
            JsonMapper jsonMapper,
            RealtimeProperties realtimeProperties,
            AppProperties appProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jsonMapper = jsonMapper;
        this.channel = realtimeProperties.channel();
        this.instanceId = appProperties.id();
    }

    public void publishMessageNew(List<UUID> targetUserIds, MessageResponse response) {
        publish(WsEventTypes.MESSAGE_NEW, targetUserIds, jsonMapper.valueToTree(response));
    }

    public void publishMessageUpdated(List<UUID> targetUserIds, MessageResponse response) {
        publish(WsEventTypes.MESSAGE_UPDATED, targetUserIds, jsonMapper.valueToTree(response));
    }

    public void publishMessageDeleted(List<UUID> targetUserIds, JsonNode payload) {
        publish(WsEventTypes.MESSAGE_DELETED, targetUserIds, payload);
    }

    private void publish(String type, List<UUID> targetUserIds, JsonNode payload) {
        WsEnvelope envelope = new WsEnvelope(type, payload);
        RealtimeEvent event = new RealtimeEvent(
                UUID.randomUUID(),
                type,
                targetUserIds,
                jsonMapper.valueToTree(envelope),
                instanceId);

        String json = jsonMapper.writeValueAsString(event);
        stringRedisTemplate.convertAndSend(channel, json);
        log.info(
                "[instance-{}] Published {} eventId={} to channel={} targets={}",
                instanceId,
                event.type(),
                event.eventId(),
                channel,
                targetUserIds.size());
    }
}
