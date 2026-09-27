package com.interviewprep.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {
    private Long id;
    private Long questionId;
    private String answerText;
    private double score;
    private double technicalAccuracy;
    private double completeness;
    private double clarity;
    private String feedback;
    private String missingConcepts;
    private String improvementSuggestion;
    private LocalDateTime createdAt;
}
