package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.api.dto.ChatMessageResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.OperationStatusResponse;
import io.github.ngtrphuc.smartphone_shop.service.ChatService;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminChatApiController {

    private final ChatService chatService;
    private final ApiMapper apiMapper;

    public AdminChatApiController(ChatService chatService, ApiMapper apiMapper) {
        this.chatService = chatService;
        this.apiMapper = apiMapper;
    }

    @GetMapping("/chat/conversations")
    public AdminConversationsResponse conversations() {
        return new AdminConversationsResponse(
                chatService.getAllConversationEmails(),
                new LinkedHashMap<>(chatService.getUnreadCountsByAdminConversation()));
    }

    @GetMapping("/chat/history")
    public List<ChatMessageResponse> chatHistory(@RequestParam(name = "email") String email) {
        return chatService.getHistory(email)
                .stream()
                .map(apiMapper::toChatMessageResponse)
                .toList();
    }

    @PostMapping("/chat/messages")
    public ChatMessageResponse sendAdminMessage(@RequestBody AdminSendMessageRequest request) {
        if (request == null || request.userEmail() == null || request.userEmail().isBlank()
                || request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Conversation email and message content are required.");
        }
        return apiMapper.toChatMessageResponse(
                chatService.saveAdminMessage(request.userEmail(), request.content()));
    }

    @PostMapping("/chat/read")
    public OperationStatusResponse markConversationRead(@RequestBody MarkReadRequest request) {
        if (request == null || request.userEmail() == null || request.userEmail().isBlank()) {
            throw new IllegalArgumentException("Conversation email is required.");
        }
        chatService.markReadByAdmin(request.userEmail());
        return new OperationStatusResponse(true, "Conversation marked as read.");
    }

    @GetMapping("/chat/unread-count")
    public long unreadCount() {
        return chatService.countAllUnreadByAdmin();
    }

    public record AdminConversationsResponse(List<String> emails, Map<String, Long> unreadCounts) {
    }

    public record AdminSendMessageRequest(String userEmail, String content) {
    }

    public record MarkReadRequest(String userEmail) {
    }
}
