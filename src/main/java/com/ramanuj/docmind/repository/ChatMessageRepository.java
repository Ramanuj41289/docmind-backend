package com.ramanuj.docmind.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ramanuj.docmind.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversation_IdOrderByCreatedAtAsc(String conversationId);

    //    For JPQL query creation
    @Query(value = """
            SELECT *
            FROM chat_messages
            WHERE conversation_id = :conversationId
            ORDER BY created_at DESC, id DESC
            LIMIT :lastN
            """, nativeQuery = true)
    List<ChatMessage> findLastNMessages(@Param("conversationId") String conversationId, @Param("lastN") int lastN);
    void deleteByConversation_Id(String conversationId);
}
