package io.github.ngtrphuc.smartphone_shop.infrastructure.websocket;

import java.util.Objects;

import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.api.dto.ChatMessageResponse;
import io.github.ngtrphuc.smartphone_shop.event.ChatMessageCreatedEvent;

@Component
public class ChatWebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;
    private final ApiMapper apiMapper;

    public ChatWebSocketNotifier(SimpMessagingTemplate messagingTemplate, ApiMapper apiMapper) {
        this.messagingTemplate = messagingTemplate;
        this.apiMapper = apiMapper;
    }

    @EventListener
    public void onMessageSaved(@NonNull ChatMessageCreatedEvent event) {
        if (event == null || event.message() == null || event.conversationEmail() == null
                || event.conversationEmail().isBlank()) {
            return;
        }
        String conversationEmail = Objects.requireNonNull(event.conversationEmail());
        ChatMessageResponse payload = Objects.requireNonNull(apiMapper.toChatMessageResponse(event.message()));
        messagingTemplate.convertAndSendToUser(conversationEmail, "/queue/chat", payload);
        messagingTemplate.convertAndSend("/topic/chat/admin", payload);
    }
}
