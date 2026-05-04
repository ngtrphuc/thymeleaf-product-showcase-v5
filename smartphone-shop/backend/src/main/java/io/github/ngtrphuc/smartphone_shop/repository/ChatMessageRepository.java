package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ngtrphuc.smartphone_shop.model.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    interface UnreadCountView {
        String getUserEmail();
        long getUnreadCount();
    }

    List<ChatMessage> findByUserEmailOrderByCreatedAtAsc(String userEmail);
    Page<ChatMessage> findByUserEmailOrderByCreatedAtDescIdDesc(String userEmail, Pageable pageable);

    @Query(value = """
            SELECT user_email FROM chat_messages
            GROUP BY user_email
            ORDER BY MAX(created_at) DESC
            """, nativeQuery = true)
    List<String> findDistinctUserEmailsOrderByLatest();

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.userEmail = :email AND m.readByAdmin = false AND m.senderRole = 'USER'")
    long countUnreadByAdmin(@Param("email") String email);

    @Query("""
            SELECT m.userEmail AS userEmail, COUNT(m) AS unreadCount
            FROM ChatMessage m
            WHERE m.readByAdmin = false AND m.senderRole = 'USER'
            GROUP BY m.userEmail
            """)
    List<UnreadCountView> countUnreadByAdminGrouped();

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.userEmail = :email AND m.readByUser = false AND m.senderRole = 'ADMIN'")
    long countUnreadByUser(@Param("email") String email);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.readByAdmin = true WHERE m.userEmail = :email AND m.senderRole = 'USER'")
    void markAllReadByAdmin(@Param("email") String email);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.readByUser = true WHERE m.userEmail = :email AND m.senderRole = 'ADMIN'")
    void markAllReadByUser(@Param("email") String email);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.readByAdmin = false AND m.senderRole = 'USER'")
    long countAllUnreadByAdmin();
}
