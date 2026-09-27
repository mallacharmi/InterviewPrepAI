package com.interviewprep.dto.ai;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIEvaluationResponse {
    private double score;
    private double technicalAccuracy;
    private double completeness;
    private double clarity;
    private String feedback;
    private List<String> missingConcepts;
    private String improvementSuggestion;
    private boolean needsFollowUp;
}
