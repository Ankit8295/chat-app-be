package com.thechat.conversation.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CreateConversationRequestValidator.class)
public @interface ValidCreateConversation {

    String message() default "Invalid create conversation request";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
