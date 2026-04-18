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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    private final List<SseEmitter> adminEmitters = new CopyOnWriteArrayList<>();

    public ChatService(ChatMessageRepository chatMessageRepository, ApplicationEventPublisher eventPublisher) {
        this.chatMessageRepository = chatMessageRepository;
        this.eventPublisher = eventPublisher;
    }

    public SseEmitter subscribeUser(String email) {
        String normalizedEmail = normalizeConversationEmail(email);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        userEmitters.computeIfAbsent(normalizedEmail, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeUserEmitter(normalizedEmail, emitter));
        emitter.onTimeout(() -> removeUserEmitter(normalizedEmail, emitter));
        emitter.onError(e -> removeUserEmitter(normalizedEmail, emitter));
        return emitter;
    }

    public SseEmitter subscribeAdmin() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        adminEmitters.add(emitter);
        emitter.onCompletion(() -> adminEmitters.remove(emitter));
        emitter.onTimeout(() -> adminEmitters.remove(emitter));
        emitter.onError(e -> adminEmitters.remove(emitter));
        return emitter;
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
        List<SseEmitter> deadAdmins = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : adminEmitters) {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException ex) {
                emitter.complete();
                deadAdmins.add(emitter);
            }
        }
        adminEmitters.removeAll(deadAdmins);

        for (Map.Entry<String, CopyOnWriteArrayList<SseEmitter>> entry : userEmitters.entrySet()) {
            String email = entry.getKey();
            CopyOnWriteArrayList<SseEmitter> emitters = entry.getValue();
            List<SseEmitter> deadUsers = new CopyOnWriteArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException ex) {
                    emitter.complete();
                    deadUsers.add(emitter);
                }
            }
            emitters.removeAll(deadUsers);
            if (emitters.isEmpty()) {
                userEmitters.remove(email, emitters);
            }
        }
    }

    private void pushToAdmins(String conversationEmail, ChatMessage msg) {
        ChatPayload payload = buildPayload(msg, conversationEmail);
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : adminEmitters) {
            try {
                sendMessageEvent(emitter, payload);
            } catch (IOException e) {
                emitter.complete();
                dead.add(emitter);
            }
        }
        adminEmitters.removeAll(dead);
    }

    private void pushToUser(String email, ChatMessage msg) {
        CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(email);
        if (emitters == null) {
            return;
        }
        ChatPayload payload = buildPayload(msg, email);
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                sendMessageEvent(emitter, payload);
            } catch (IOException e) {
                emitter.complete();
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
        if (emitters.isEmpty()) {
            userEmitters.remove(email, emitters);
        }
    }

    private void removeUserEmitter(String email, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(email);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(email, emitters);
            }
        }
    }

    private String normalizeMessageContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message content is too long.");
        }
        return normalized;
    }

    private String normalizeConversationEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Conversation email cannot be empty.");
        }
        if (normalized.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("Conversation email is too long.");
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Conversation email is invalid.");
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
