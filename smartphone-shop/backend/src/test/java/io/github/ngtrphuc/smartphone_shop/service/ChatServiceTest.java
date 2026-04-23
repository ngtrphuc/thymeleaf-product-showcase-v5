package io.github.ngtrphuc.smartphone_shop.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import io.github.ngtrphuc.smartphone_shop.common.exception.ValidationException;
import io.github.ngtrphuc.smartphone_shop.event.ChatMessageCreatedEvent;
import io.github.ngtrphuc.smartphone_shop.model.ChatMessage;
import io.github.ngtrphuc.smartphone_shop.repository.ChatMessageRepository;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ChatSseRegistry chatSseRegistry;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatSseRegistry = new ChatSseRegistry();
        chatService = new ChatService(chatMessageRepository, eventPublisher, chatSseRegistry);
    }

    @Test
    void saveUserMessage_shouldNormalizeAndPublishEvent() {
        when(chatMessageRepository.save(MockitoNullSafety.anyNonNull(ChatMessage.class)))
                .thenAnswer(invocation -> {
                    ChatMessage saved = invocation.getArgument(0, ChatMessage.class);
                    saved.setId(11L);
                    saved.setCreatedAt(LocalDateTime.of(2026, 4, 18, 12, 0));
                    return saved;
                });

        ChatMessage saved = chatService.saveUserMessage(" USER@EXAMPLE.COM ", "  hello admin  ");

        assertEquals(11L, saved.getId());
        assertEquals("user@example.com", saved.getUserEmail());
        assertEquals("hello admin", saved.getContent());
        assertEquals("USER", saved.getSenderRole());
        assertEquals(false, saved.isReadByAdmin());
        assertEquals(true, saved.isReadByUser());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(MockitoNullSafety.captureNonNull(eventCaptor));
        Object published = MockitoNullSafety.capturedValue(eventCaptor);
        assertTrue(published instanceof ChatMessageCreatedEvent);
        ChatMessageCreatedEvent event = (ChatMessageCreatedEvent) published;
        assertEquals("user@example.com", event.conversationEmail());
        assertEquals(11L, event.message().getId());
    }

    @Test
    void saveAdminMessage_shouldSetAdminFlagsAndPublishEvent() {
        when(chatMessageRepository.save(MockitoNullSafety.anyNonNull(ChatMessage.class)))
                .thenAnswer(invocation -> {
                    ChatMessage saved = invocation.getArgument(0, ChatMessage.class);
                    saved.setId(12L);
                    saved.setCreatedAt(LocalDateTime.of(2026, 4, 18, 12, 5));
                    return saved;
                });

        ChatMessage saved = chatService.saveAdminMessage("user@example.com", "Please check your order.");

        assertEquals("ADMIN", saved.getSenderRole());
        assertEquals(true, saved.isReadByAdmin());
        assertEquals(false, saved.isReadByUser());
        verify(eventPublisher).publishEvent(MockitoNullSafety.anyNonNull(ChatMessageCreatedEvent.class));
    }

    @Test
    void getHistory_shouldReturnOldestFirstFromLatestPage() {
        ChatMessage latest = chat(2L, "user@example.com", "latest", LocalDateTime.of(2026, 4, 18, 13, 0));
        ChatMessage older = chat(1L, "user@example.com", "older", LocalDateTime.of(2026, 4, 18, 12, 0));

        when(chatMessageRepository.findByUserEmailOrderByCreatedAtDesc(
                eq("user@example.com"),
                eq(PageRequest.of(0, 50))))
                .thenReturn(new PageImpl<>(Objects.requireNonNull(List.of(latest, older))));

        List<ChatMessage> history = chatService.getHistory("user@example.com");

        assertEquals(2, history.size());
        assertEquals("older", history.getFirst().getContent());
        assertEquals("latest", history.get(1).getContent());
    }

    @Test
    void getUnreadCountsByAdminConversation_shouldMapRowsInOrder() {
        ChatMessageRepository.UnreadCountView first = unread("user-a@example.com", 3L);
        ChatMessageRepository.UnreadCountView second = unread("user-b@example.com", 1L);
        when(chatMessageRepository.countUnreadByAdminGrouped())
                .thenReturn(Objects.requireNonNull(List.of(first, second)));

        Map<String, Long> unreadCounts = chatService.getUnreadCountsByAdminConversation();

        assertEquals(2, unreadCounts.size());
        assertEquals(3L, unreadCounts.get("user-a@example.com"));
        assertEquals(1L, unreadCounts.get("user-b@example.com"));
    }

    @Test
    void markReadByAdmin_shouldNormalizeEmailBeforeDelegating() {
        chatService.markReadByAdmin(" USER@EXAMPLE.COM ");
        verify(chatMessageRepository).markAllReadByAdmin("user@example.com");
    }

    @Test
    void saveUserMessage_shouldRejectBlankContent() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> chatService.saveUserMessage("user@example.com", "   "));
        assertEquals("Message content cannot be empty.", exception.getMessage());
    }

    @Test
    void saveAdminMessage_shouldRejectInvalidConversationEmail() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> chatService.saveAdminMessage("not-an-email", "hello"));
        assertEquals("Conversation email is invalid.", exception.getMessage());
    }

    private ChatMessage chat(Long id, String email, String content, LocalDateTime createdAt) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setUserEmail(email);
        message.setContent(content);
        message.setSenderRole("USER");
        message.setCreatedAt(createdAt);
        return message;
    }

    private ChatMessageRepository.UnreadCountView unread(String email, long count) {
        return new ChatMessageRepository.UnreadCountView() {
            @Override
            public String getUserEmail() {
                return email;
            }

            @Override
            public long getUnreadCount() {
                return count;
            }
        };
    }
}
