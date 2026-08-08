package com.thechat.realtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.realtime")
public record RealtimeProperties(String channel) {
}
