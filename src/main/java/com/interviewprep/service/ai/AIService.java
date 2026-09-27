package com.interviewprep.service.ai;

import com.interviewprep.dto.ai.AIEvaluationRequest;
import com.interviewprep.dto.ai.AIEvaluationResponse;
import com.interviewprep.dto.ai.AIFollowUpRequest;
import com.interviewprep.dto.ai.AIFollowUpResponse;
import com.interviewprep.dto.ai.AIQuestionRequest;
import com.interviewprep.dto.ai.AIQuestionResponse;

public interface AIService {
    AIQuestionResponse generateQuestion(AIQuestionRequest request);
    AIEvaluationResponse evaluateAnswer(AIEvaluationRequest request);
    AIFollowUpResponse generateFollowUp(AIFollowUpRequest request);
    String generateChatResponse(String systemPrompt, String userMessage, java.util.List<com.interviewprep.dto.ai.ChatMessageDto> history);
    boolean isAvailable();
}
