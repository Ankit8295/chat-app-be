package com.thechat.message;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thechat.message.dto.MessagePageResponse;

@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ResponseEntity<MessagePageResponse> getMessages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID conversationId,
            @RequestParam(value = "prevCursor", required = false) String prevCursor,
            @RequestParam(value = "nextCursor", required = false) String nextCursor,
            @RequestParam(value = "limit", required = false) Integer limit) {
        UUID currentUserId = UUID.fromString(jwt.getClaimAsString("userId"));
        MessagePageResponse page = messageService.getMessages(
                conversationId,
                currentUserId,
                prevCursor,
                nextCursor,
                limit);
        return ResponseEntity.ok(page);
    }
}
