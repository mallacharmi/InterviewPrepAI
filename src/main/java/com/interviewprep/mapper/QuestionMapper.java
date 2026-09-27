package com.interviewprep.mapper;
import com.interviewprep.dto.response.QuestionResponse;
import com.interviewprep.entity.Question;
import org.springframework.stereotype.Component;

@Component
public class QuestionMapper {
    public QuestionResponse toResponse(Question question) {
        if (question == null) {
            return null;
        }
        String diffStr = "MEDIUM";
        if (question.getDifficulty() != null) {
            diffStr = question.getDifficulty().name();
        } else if (question.getInterview() != null && question.getInterview().getDifficulty() != null) {
            diffStr = question.getInterview().getDifficulty().name();
        }

        int completed = question.getInterview() != null ? question.getInterview().getCompletedQuestions() : 0;
        int displayNum = question.isFollowUp() ? Math.max(1, completed) : (completed + 1);

        return QuestionResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .topic(question.getTopic() != null ? question.getTopic() : "General")
                .difficulty(diffStr)
                .questionType(question.getQuestionType() != null ? question.getQuestionType().name() : "TECHNICAL")
                .sequenceNumber(question.getSequenceNumber())
                .displayQuestionNumber(displayNum)
                .isFollowUp(question.isFollowUp())
                .build();
    }
}
