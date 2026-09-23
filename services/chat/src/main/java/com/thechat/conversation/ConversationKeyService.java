package com.thechat.conversation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thechat.conversation.dto.ConversationKeyEnvelopeResponse;
import com.thechat.conversation.dto.ConversationKeysResponse;
import com.thechat.conversation.dto.CreateConversationRequest;
import com.thechat.conversation.dto.KeyEnvelopeRequest;
import com.thechat.conversation.dto.PutConversationKeysRequest;

@Service
public class ConversationKeyService {

    private final ConversationKeyEnvelopeRepository envelopeRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;

    public ConversationKeyService(
            ConversationKeyEnvelopeRepository envelopeRepository,
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository) {
        this.envelopeRepository = envelopeRepository;
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
    }

    @Transactional
    public void saveRequired(UUID conversationId, Set<UUID> participantIds, CreateConversationRequest request) {
        requireEnvelopes(request);
        saveIfAbsent(conversationId, participantIds, request.keyVersion(), request.envelopes());
    }

    @Transactional
    public void saveIfMissing(UUID conversationId, Set<UUID> participantIds, CreateConversationRequest request) {
        if (request.keyVersion() == null || request.envelopes() == null || request.envelopes().isEmpty()) {
            return;
        }
        saveIfAbsent(conversationId, participantIds, request.keyVersion(), request.envelopes());
    }

    @Transactional(readOnly = true)
    public ConversationKeysResponse getOwn(UUID conversationId, UUID currentUserId) {
        assertParticipant(conversationId, currentUserId);
        List<ConversationKeyEnvelopeResponse> envelopes = envelopeRepository
                .findByConversationIdAndUserId(conversationId, currentUserId)
                .stream()
                .map(ConversationKeyEnvelopeResponse::from)
                .toList();
        return new ConversationKeysResponse(envelopes);
    }

    @Transactional
    public ConversationKeysResponse putEnvelopes(
            UUID conversationId,
            UUID currentUserId,
            PutConversationKeysRequest request) {
        assertParticipant(conversationId, currentUserId);
        Set<UUID> participantIds = new HashSet<>(
                participantRepository.findUserIdsByConversationId(conversationId));
        saveIfAbsent(conversationId, participantIds, request.keyVersion(), request.envelopes());
        return getOwn(conversationId, currentUserId);
    }

    private void saveIfAbsent(
            UUID conversationId,
            Set<UUID> participantIds,
            Integer keyVersion,
            List<KeyEnvelopeRequest> envelopes) {
        validateEnvelopes(participantIds, keyVersion, envelopes);
        if (envelopeRepository.existsByConversationIdAndKeyVersion(conversationId, keyVersion)) {
            return;
        }

        List<ConversationKeyEnvelope> entities = envelopes.stream()
                .map(envelope -> new ConversationKeyEnvelope(
                        conversationId,
                        envelope.userId(),
                        keyVersion,
                        envelope.wrappedKey(),
                        envelope.wrapNonce(),
                        envelope.ephPublicKey()))
                .toList();

        try {
            envelopeRepository.saveAll(entities);
            envelopeRepository.flush();
        } catch (DataIntegrityViolationException ignored) {
            // Another opener won the race for this key version.
        }
    }

    private static void requireEnvelopes(CreateConversationRequest request) {
        if (request.keyVersion() == null || request.envelopes() == null || request.envelopes().isEmpty()) {
            throw new IllegalArgumentException("key envelopes are required");
        }
    }

    private static void validateEnvelopes(
            Set<UUID> participantIds,
            Integer keyVersion,
            List<KeyEnvelopeRequest> envelopes) {
        if (keyVersion == null || keyVersion < 1) {
            throw new IllegalArgumentException("keyVersion must be at least 1");
        }
        if (envelopes == null || envelopes.isEmpty()) {
            throw new IllegalArgumentException("key envelopes are required");
        }

        Set<UUID> envelopeUserIds = envelopes.stream()
                .map(KeyEnvelopeRequest::userId)
                .collect(Collectors.toCollection(HashSet::new));
        if (envelopeUserIds.size() != envelopes.size()) {
            throw new IllegalArgumentException("duplicate envelope userId");
        }
        if (!envelopeUserIds.equals(participantIds)) {
            throw new IllegalArgumentException("envelope userIds must match conversation participants");
        }
    }

    private void assertParticipant(UUID conversationId, UUID currentUserId) {
        if (!conversationRepository.existsById(conversationId)
                || !participantRepository.existsByConversationIdAndUserId(conversationId, currentUserId)) {
            throw new ConversationNotFoundException(conversationId);
        }
    }
}
