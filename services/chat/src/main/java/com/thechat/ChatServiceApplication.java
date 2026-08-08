package com.thechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Phase 4: Chat is now a standalone Spring Boot application — the last of the
 * three original bounded contexts to be extracted from the monolith.
 * Owns: conversations, messages, WebSocket, Redis fan-out. Own DB (chat_db).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}
