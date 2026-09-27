package com.interviewprep.dto.ai;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIQuestionRequest {
    private String targetRole;
    private String topic;
    private String difficulty;
    private String interviewType;
    private String experienceLevel;
    private int questionNumber;
    private int totalQuestions;
}
