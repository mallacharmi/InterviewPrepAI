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
public class AIFollowUpResponse {
    private String followUpQuestion;
    private String topic;
    private List<String> expectedConcepts;

    public String getQuestion() { return followUpQuestion; }
    public void setQuestion(String q) { this.followUpQuestion = q; }
}
