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
public class AIFollowUpRequest {
    private String originalQuestion;
    private String candidateAnswer;
    private String feedback;
    private String topic;
    private String difficulty;
    private List<String> weakAreas;

    public String getUserAnswer() { return candidateAnswer; }
    public void setUserAnswer(String ua) { this.candidateAnswer = ua; }
}
