package io.github.ngtrphuc.smartphone_shop.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.github.ngtrphuc.smartphone_shop.common.exception.ValidationException;
import io.github.ngtrphuc.smartphone_shop.event.ChatMessageCreatedEvent;
import io.github.ngtrphuc.smartphone_shop.model.ChatMessage;
import io.github.ngtrphuc.smartphone_shop.repository.ChatMessageRepository;

@Service
public class ChatService {
    private static final long SSE_TIMEOUT_MS = 300_000L;
    private static final long SSE_HEARTBEAT_MS = 60_000L;
    private static final int DEFAULT_HISTORY_LIMIT = 50;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final ChatMessageRepository chatMessageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatSseRegistry chatSseRegistry;

    public ChatService(ChatMessageRepository chatMessageRepository,
            ApplicationEventPublisher eventPublisher,
            ChatSseRegistry chatSseRegistry) {
        this.chatMessageRepository = chatMessageRepository;
        this.eventPublisher = eventPublisher;
        this.chatSseRegistry = chatSseRegistry;
    }

    public SseEmitter subscribeUser(String email) {
        String normalizedEmail = normalizeConversationEmail(email);
        return chatSseRegistry.subscribeUser(normalizedEmail, SSE_TIMEOUT_MS);
    }

    public SseEmitter subscribeAdmin() {
        return chatSseRegistry.subscribeAdmin(SSE_TIMEOUT_MS);
    }

    @Transactional
    public ChatMessage saveUserMessage(String email, String content) {
        String normalizedEmail = normalizeConversationEmail(email);
        ChatMessage msg = new ChatMessage();
        msg.setUserEmail(normalizedEmail);
        msg.setContent(normalizeMessageContent(content));
        msg.setSenderRole("USER");
        msg.setReadByAdmin(false);
        msg.setReadByUser(true);
        ChatMessage saved = chatMessageRepository.save(msg);
        eventPublisher.publishEvent(new ChatMessageCreatedEvent(normalizedEmail, saved));
        pushToAdmins(normalizedEmail, saved);
        return saved;
    }

    @Transactional
    public ChatMessage saveAdminMessage(String userEmail, String content) {
        String normalizedEmail = normalizeConversationEmail(userEmail);
        ChatMessage msg = new ChatMessage();
        msg.setUserEmail(normalizedEmail);
        msg.setContent(normalizeMessageContent(content));
        msg.setSenderRole("ADMIN");
        msg.setReadByAdmin(true);
        msg.setReadByUser(false);
        ChatMessage saved = chatMessageRepository.save(msg);
        eventPublisher.publishEvent(new ChatMessageCreatedEvent(normalizedEmail, saved));
        pushToUser(normalizedEmail, saved);
        pushToAdmins(normalizedEmail, saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getHistory(String email) {
        List<ChatMessage> latestFirst = new ArrayList<>(chatMessageRepository
                .findByUserEmailOrderByCreatedAtDesc(
                        normalizeConversationEmail(email),
                        PageRequest.of(0, DEFAULT_HISTORY_LIMIT))
                .getContent());
        Collections.reverse(latestFirst);
        return latestFirst;
    }

    @Transactional(readOnly = true)
    public List<String> getAllConversationEmails() {
        return chatMessageRepository.findDistinctUserEmailsOrderByLatest();
    }

    @Transactional(readOnly = true)
    public long countUnreadByAdmin(String email) {
        return chatMessageRepository.countUnreadByAdmin(normalizeConversationEmail(email));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getUnreadCountsByAdminConversation() {
        Map<String, Long> unreadCounts = new LinkedHashMap<>();
        for (ChatMessageRepository.UnreadCountView row : chatMessageRepository.countUnreadByAdminGrouped()) {
            unreadCounts.put(row.getUserEmail(), row.getUnreadCount());
        }
        return unreadCounts;
    }

    @Transactional(readOnly = true)
    public long countUnreadByUser(String email) {
        return chatMessageRepository.countUnreadByUser(normalizeConversationEmail(email));
    }

    @Transactional(readOnly = true)
    public long countAllUnreadByAdmin() {
        return chatMessageRepository.countAllUnreadByAdmin();
    }

    @Transactional
    public void markReadByAdmin(String email) {
        chatMessageRepository.markAllReadByAdmin(normalizeConversationEmail(email));
    }

    @Transactional
    public void markReadByUser(String email) {
        chatMessageRepository.markAllReadByUser(normalizeConversationEmail(email));
    }

    @Scheduled(fixedDelay = SSE_HEARTBEAT_MS)
    public void pruneDeadEmitters() {
        chatSseRegistry.heartbeatAll(emitter -> emitter.send(SseEmitter.event().comment("heartbeat")));
    }

    private void pushToAdmins(String conversationEmail, ChatMessage msg) {
        ChatPayload payload = buildPayload(msg, conversationEmail);
        chatSseRegistry.withEachAdminEmitter(emitter -> sendMessageEvent(emitter, payload));
    }

    private void pushToUser(String email, ChatMessage msg) {
        ChatPayload payload = buildPayload(msg, email);
        chatSseRegistry.withEachUserEmitter(email, emitter -> sendMessageEvent(emitter, payload));
    }

    private String normalizeMessageContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            throw new ValidationException("Message content cannot be empty.");
        }
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            throw new ValidationException("Message content is too long.");
        }
        return normalized;
    }

    private String normalizeConversationEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ValidationException("Conversation email cannot be empty.");
        }
        if (normalized.length() > MAX_EMAIL_LENGTH) {
            throw new ValidationException("Conversation email is too long.");
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new ValidationException("Conversation email is invalid.");
        }
        return normalized;
    }

    private void sendMessageEvent(SseEmitter emitter, ChatPayload payload) throws IOException {
        emitter.send(SseEmitter.event()
                .name("message")
                .data(Objects.requireNonNull(payload)));
    }

    private ChatPayload buildPayload(ChatMessage msg, String conversationEmail) {
        return new ChatPayload(
                msg.getId(),
                conversationEmail,
                msg.getContent(),
                msg.getSenderRole(),
                msg.getCreatedAt());
    }

    private record ChatPayload(Long id, String userEmail, String content, String senderRole, LocalDateTime createdAt) {
    }
}
