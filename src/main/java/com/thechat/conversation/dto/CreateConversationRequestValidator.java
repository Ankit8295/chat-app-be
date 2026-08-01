package com.thechat.conversation.dto;

import com.thechat.conversation.ConversationType;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CreateConversationRequestValidator
        implements ConstraintValidator<ValidCreateConversation, CreateConversationRequest> {

    @Override
    public boolean isValid(CreateConversationRequest request, ConstraintValidatorContext context) {
        if (request == null || request.type() == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        if (request.type() == ConversationType.DIRECT) {
            if (request.userId() == null) {
                addViolation(context, "userId", "must not be null");
                valid = false;
            }
        } else if (request.type() == ConversationType.GROUP) {
            if (request.name() == null || request.name().isBlank()) {
                addViolation(context, "name", "must not be blank");
                valid = false;
            }
            if (request.participants() == null || request.participants().isEmpty()) {
                addViolation(context, "participants", "must not be empty");
                valid = false;
            }
        }

        return valid;
    }

    private static void addViolation(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}
