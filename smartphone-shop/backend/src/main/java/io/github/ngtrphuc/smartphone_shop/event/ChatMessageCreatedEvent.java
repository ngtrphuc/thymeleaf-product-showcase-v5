package io.github.ngtrphuc.smartphone_shop.event;

import io.github.ngtrphuc.smartphone_shop.model.ChatMessage;

public record ChatMessageCreatedEvent(
        String conversationEmail,
        ChatMessage message) {
}
