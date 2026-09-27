package com.interviewprep.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResponse {
    private Long questionId;
    private String questionText;
    private String answerText;
    private double score;
    private double technicalAccuracy;
    private double completeness;
    private double clarity;
    private String feedback;
    private List<String> missingConcepts;
    private String improvementSuggestion;
    private boolean hasFollowUp;
    private String followUpQuestion;

    public String getFollowUpQuestion() { return followUpQuestion; }
    public void setFollowUpQuestion(String followUpQuestion) { this.followUpQuestion = followUpQuestion; }

    public double getTechnicalAccuracyScore() { return technicalAccuracy; }
    public double getCompletenessScore() { return completeness; }
    public double getClarityScore() { return clarity; }

    public void setTechnicalAccuracyScore(double val) { this.technicalAccuracy = val; }
    public void setCompletenessScore(double val) { this.completeness = val; }
    public void setClarityScore(double val) { this.clarity = val; }
}
