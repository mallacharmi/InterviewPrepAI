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
public class QuestionResultDTO {
    private Long questionId;
    private String questionText;
    private String topic;
    private String difficulty;
    private String answerText;
    private double score;
    private String feedback;
    private List<String> missingConcepts;
    private String improvementSuggestion;
}
