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
public class AIEvaluationRequest {
    private String questionText;
    private String answerText;
    private String targetRole;
    private String difficulty;
    private List<String> expectedConcepts;
    private String topic;

    public String getQuestion() { return questionText; }
    public void setQuestion(String q) { this.questionText = q; }
    public String getAnswer() { return answerText; }
    public void setAnswer(String a) { this.answerText = a; }
}
