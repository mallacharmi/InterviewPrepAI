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
public class InterviewResponse {
    private Long id;
    private String targetRole;
    private String interviewType;
    private String difficulty;
    private String status;
    private int totalQuestions;
    private int completedQuestions;
    private Double overallScore;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String videoRecordingUrl;
    private Double confidenceScore;
    private Double fearScore;
    private Double shyScore;
    private Double calmScore;
    private String expressionSummary;
    private String terminationReason;
    private Integer violationCount;
    private LocalDateTime createdAt;
}
