package com.interviewprep.mapper;
import com.interviewprep.dto.response.InterviewResponse;
import com.interviewprep.entity.Interview;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class InterviewMapper {
    public InterviewResponse toResponse(Interview interview) {
        if (interview == null) {
            return null;
        }
        return InterviewResponse.builder()
                .id(interview.getId())
                .targetRole(interview.getTargetRole())
                .interviewType(interview.getInterviewType() != null ? interview.getInterviewType().name() : null)
                .difficulty(interview.getDifficulty() != null ? interview.getDifficulty().name() : null)
                .status(interview.getStatus() != null ? interview.getStatus().name() : null)
                .totalQuestions(interview.getTotalQuestions())
                .completedQuestions(interview.getCompletedQuestions())
                .overallScore(interview.getOverallScore())
                .startedAt(interview.getStartedAt())
                .completedAt(interview.getCompletedAt())
                .videoRecordingUrl(interview.getVideoRecordingUrl())
                .confidenceScore(interview.getConfidenceScore())
                .fearScore(interview.getFearScore())
                .shyScore(interview.getShyScore())
                .calmScore(interview.getCalmScore())
                .expressionSummary(interview.getExpressionSummary())
                .terminationReason(interview.getTerminationReason())
                .violationCount(interview.getViolationCount())
                .createdAt(interview.getCreatedAt())
                .build();
    }

    public List<InterviewResponse> toResponseList(List<Interview> interviews) {
        if (interviews == null) {
            return null;
        }
        return interviews.stream().map(this::toResponse).collect(Collectors.toList());
    }
}
