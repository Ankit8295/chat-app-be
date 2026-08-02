package com.thechat.message;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thechat.conversation.Conversation;
import com.thechat.conversation.ConversationRepository;

@Service
public class MessagePersistenceService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    public MessagePersistenceService(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
    }

    @Transactional
    public void saveMessageAndTouchConversation(Message message, Conversation conversation) {
        messageRepository.save(message);
        conversation.touch();
        conversationRepository.save(conversation);
    }
}
