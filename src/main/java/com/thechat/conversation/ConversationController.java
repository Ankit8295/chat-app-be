package com.thechat.conversation;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thechat.conversation.dto.ConversationDetailResponse;
import com.thechat.conversation.dto.ConversationResponse;
import com.thechat.conversation.dto.CreateConversationRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getConversations(@AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getClaimAsString("userId"));
        List<ConversationResponse> conversations = conversationService.getUserConversations(currentUserId);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationDetailResponse> getConversation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID conversationId) {
        UUID currentUserId = UUID.fromString(jwt.getClaimAsString("userId"));
        ConversationDetailResponse conversation = conversationService.getUserConversation(conversationId,
                currentUserId);
        return ResponseEntity.ok(conversation);
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateConversationRequest request) {
        UUID currentUserId = UUID.fromString(jwt.getClaimAsString("userId"));
        ConversationResponse conversation = conversationService.createDirectConversation(currentUserId,
                request.userId());
        return ResponseEntity.ok(conversation);
    }
}
