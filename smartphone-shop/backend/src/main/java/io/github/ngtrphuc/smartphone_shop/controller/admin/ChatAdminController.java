package io.github.ngtrphuc.smartphone_shop.controller.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.transaction.annotation.Transactional;

import io.github.ngtrphuc.smartphone_shop.service.ChatService;

@Controller
public class ChatAdminController {

    private final ChatService chatService;

    public ChatAdminController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/admin/chat")
    public String adminChatPage(Model model) {
        List<String> emails = chatService.getAllConversationEmails();
        Map<String, Long> unreadCounts = new HashMap<>(chatService.getUnreadCountsByAdminConversation());
        model.addAttribute("emails", emails);
        model.addAttribute("unreadCounts", unreadCounts);
        model.addAttribute("selectedEmail", null);
        return "chat";
    }

    @GetMapping("/admin/chat/{email}")
    public String adminChatConversation(@PathVariable String email, Model model) {
        try {
            model.addAttribute("history", chatService.getHistory(email));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("emails", chatService.getAllConversationEmails());
            model.addAttribute("unreadCounts", new HashMap<>(chatService.getUnreadCountsByAdminConversation()));
            model.addAttribute("selectedEmail", null);
            model.addAttribute("toast", "Conversation not found.");
            return "chat";
        }
        List<String> emails = chatService.getAllConversationEmails();
        Map<String, Long> unreadCounts = new HashMap<>(chatService.getUnreadCountsByAdminConversation());
        model.addAttribute("emails", emails);
        model.addAttribute("unreadCounts", unreadCounts);
        model.addAttribute("selectedEmail", email);
        return "chat";
    }

    @GetMapping(value = "/admin/chat/stream", produces = "text/event-stream")
    @ResponseBody
    public SseEmitter streamAdmin() {
        return chatService.subscribeAdmin();
    }

    @PostMapping("/admin/chat/send")
    @ResponseBody
    @Transactional
    public ResponseEntity<String> sendAdminMessage(@RequestParam String userEmail, @RequestParam String content) {
        if (content == null || content.isBlank() || userEmail == null) {
            return ResponseEntity.badRequest().body("error");
        }
        try {
            chatService.saveAdminMessage(userEmail.trim(), content);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("error");
        }
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/admin/chat/unread-count")
    @ResponseBody
    public long adminUnreadCount() {
        return chatService.countAllUnreadByAdmin();
    }

    @GetMapping("/admin/chat/unread-counts")
    @ResponseBody
    public Map<String, Long> adminUnreadCounts() {
        return new HashMap<>(chatService.getUnreadCountsByAdminConversation());
    }

    @PostMapping("/admin/chat/mark-read")
    @ResponseBody
    public ResponseEntity<String> markConversationRead(@RequestParam String userEmail) {
        try {
            chatService.markReadByAdmin(userEmail);
            return ResponseEntity.ok("ok");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("error");
        }
    }
}
