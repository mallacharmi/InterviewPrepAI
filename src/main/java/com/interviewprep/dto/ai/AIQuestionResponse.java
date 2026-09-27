package com.interviewprep.dto.ai;
import com.interviewprep.entity.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIQuestionResponse {
    private String questionText;
    private String topic;
    private String difficulty;
    private QuestionType questionType;
    private List<String> expectedConcepts;

    public AIQuestionResponse(String questionText, String topic, String difficulty, List<String> expectedConcepts) {
        this.questionText = questionText;
        this.topic = topic;
        this.difficulty = difficulty;
        this.expectedConcepts = expectedConcepts;
        this.questionType = QuestionType.TECHNICAL;
    }

    public String getQuestion() {
        return questionText;
    }

    public void setQuestion(String question) {
        this.questionText = question;
    }
}
