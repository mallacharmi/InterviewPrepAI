package com.interviewprep.repository;

import com.interviewprep.entity.ChatMessage;
import com.interviewprep.entity.enums.ChatContextType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUserIdAndContextTypeAndContextIdOrderByCreatedAtAsc(
            Long userId, ChatContextType contextType, Long contextId);

    List<ChatMessage> findByUserIdAndContextTypeAndContextIdNullOrderByCreatedAtAsc(
            Long userId, ChatContextType contextType);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.user.id = :userId AND m.contextType = :contextType AND m.contextId = :contextId")
    void deleteByUserIdAndContextTypeAndContextId(@Param("userId") Long userId, @Param("contextType") ChatContextType contextType, @Param("contextId") Long contextId);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.user.id = :userId AND m.contextType = :contextType AND m.contextId IS NULL")
    void deleteByUserIdAndContextTypeAndContextIdNull(@Param("userId") Long userId, @Param("contextType") ChatContextType contextType);
}
