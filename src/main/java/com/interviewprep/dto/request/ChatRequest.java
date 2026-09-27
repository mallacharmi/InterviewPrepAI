package com.interviewprep.dto.request;

import com.interviewprep.dto.ai.ChatMessageDto;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "Context type is required (PREP, REVIEW, or RESUME)")
    private String contextType;

    private Long contextId; // sessionId for REVIEW, resumeAnalysisId/null for RESUME/PREP

    @NotBlank(message = "Message content cannot be blank")
    private String message;

    // Optional context params for PREP mode
    private String targetRole;
    private String difficulty;
    private List<String> topics;

    // Optional context params for RESUME mode
    private String resumeText;
    private String jobDescription;
    private Double atsScore;
    private List<String> missingKeywords;

    // Optional client-side history fallback (trimmed to last 5-10 turns)
    private List<ChatMessageDto> conversationHistory;
}
