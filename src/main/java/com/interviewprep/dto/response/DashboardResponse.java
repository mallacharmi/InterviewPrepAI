package com.interviewprep.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private long totalInterviews;
    private long completedInterviews;
    private long terminatedInterviews;
    private long totalViolations;
    private long totalQuestionsAnswered;
    private int currentStreak;
    private int maxStreak;
    private double averageScore;
    private double highestScore;
    private List<InterviewResponse> recentInterviews;
    private Map<String, Double> topicPerformance;
    private List<String> strongestTopics;
    private List<String> weakestTopics;
}
