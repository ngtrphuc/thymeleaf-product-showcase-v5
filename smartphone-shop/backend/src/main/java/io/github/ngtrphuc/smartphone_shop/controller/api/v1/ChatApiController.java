package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.dto.*;
import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.model.ChatMessage;
import io.github.ngtrphuc.smartphone_shop.service.ChatService;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatApiController {

    private final ChatService chatService;
    private final ApiMapper apiMapper;

    public ChatApiController(ChatService chatService, ApiMapper apiMapper) {
        this.chatService = chatService;
        this.apiMapper = apiMapper;
    }

    @GetMapping("/history")
    public List<ChatMessageResponse> history(Authentication authentication) {
        return chatService.getHistory(authentication.getName()).stream()
                .map(apiMapper::toChatMessageResponse)
                .toList();
    }

    @PostMapping("/messages")
    public ChatMessageResponse send(Authentication authentication, @RequestBody ChatSendRequest request) {
        ChatMessage saved = chatService.saveUserMessage(authentication.getName(), request.content());
        return apiMapper.toChatMessageResponse(saved);
    }

    @GetMapping("/unread-count")
    public long unreadCount(Authentication authentication) {
        return chatService.countUnreadByUser(authentication.getName());
    }

    @PostMapping("/read")
    public OperationStatusResponse markRead(Authentication authentication) {
        chatService.markReadByUser(authentication.getName());
        return new OperationStatusResponse(true, "Conversation marked as read.");
    }

    private record ChatSendRequest(String content) {
    }
}

