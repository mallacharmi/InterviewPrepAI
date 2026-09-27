package com.interviewprep.controller.api;

import com.interviewprep.dto.ai.ChatMessageDto;
import com.interviewprep.dto.request.ChatRequest;
import com.interviewprep.dto.response.ChatResponse;
import com.interviewprep.security.CustomUserDetails;
import com.interviewprep.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChatRequest request) {
        log.info("REST request for AI Chatbot from user ID: {}, context: {}", userDetails.getId(), request.getContextType());
        ChatResponse response = chatService.processChatMessage(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessageDto>> getHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("contextType") String contextType,
            @RequestParam(value = "contextId", required = false) Long contextId) {
        log.info("REST request to get chat history for user ID: {}, context: {}", userDetails.getId(), contextType);
        List<ChatMessageDto> history = chatService.getChatHistory(userDetails.getId(), contextType, contextId);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("contextType") String contextType,
            @RequestParam(value = "contextId", required = false) Long contextId) {
        log.info("REST request to clear chat history for user ID: {}, context: {}, contextId: {}", userDetails.getId(), contextType, contextId);
        chatService.clearChatHistory(userDetails.getId(), contextType, contextId);
        return ResponseEntity.noContent().build();
    }
}
