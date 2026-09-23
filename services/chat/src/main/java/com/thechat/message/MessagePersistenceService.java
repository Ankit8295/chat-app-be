package com.thechat.message;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thechat.conversation.Conversation;
import com.thechat.conversation.ConversationParticipantRepository;
import com.thechat.conversation.ConversationRepository;

@Service
public class MessagePersistenceService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;

    public MessagePersistenceService(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            ConversationParticipantRepository conversationParticipantRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
    }

    @Transactional
    public void saveMessageAndTouchConversation(Message message, Conversation conversation) {
        messageRepository.save(message);
        conversation.touch();
        conversationRepository.save(conversation);
        conversationParticipantRepository.clearHiddenAtByConversationId(conversation.getId());
    }
}
