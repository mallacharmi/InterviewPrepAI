package com.interviewprep.mapper;
import com.interviewprep.dto.response.AnswerResponse;
import com.interviewprep.entity.Answer;
import org.springframework.stereotype.Component;

@Component
public class AnswerMapper {
    public AnswerResponse toResponse(Answer answer) {
        if (answer == null) {
            return null;
        }
        return AnswerResponse.builder()
                .id(answer.getId())
                .questionId(answer.getQuestion() != null ? answer.getQuestion().getId() : null)
                .answerText(answer.getAnswerText())
                .score(answer.getScore())
                .technicalAccuracy(answer.getTechnicalAccuracy())
                .completeness(answer.getCompleteness())
                .clarity(answer.getClarity())
                .feedback(answer.getFeedback())
                .missingConcepts(answer.getMissingConcepts())
                .improvementSuggestion(answer.getImprovementSuggestion())
                .createdAt(answer.getCreatedAt())
                .build();
    }
}
