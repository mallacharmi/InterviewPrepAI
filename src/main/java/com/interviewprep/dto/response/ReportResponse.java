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
public class ReportResponse {
    private Long interviewId;
    private String targetRole;
    private String interviewType;
    private String difficulty;
    private double overallScore;
    private int totalQuestions;
    private double averageTechnicalAccuracy;
    private double averageCompleteness;
    private double averageClarity;
    private Map<String, Double> topicScores;
    private List<String> strongTopics;
    private List<String> weakTopics;
    private List<String> missingSkills;
    private List<String> suggestions;
    private List<CourseItem> recommendedCourses;
    private List<QuestionResultDTO> questionResults;
    private Double confidenceScore;
    private Double fearScore;
    private Double shyScore;
    private Double calmScore;
    private String expressionSummary;
    private List<String> expressionFeedback;

    private Integer tabSwitchCount;
    private Integer fullscreenExitCount;
    private Integer externalDeviceCount;
    private Integer noFaceDetectedCount;
    private Integer eyesClosedCount;
    private Integer headTurnedCount;
    private Integer gazeOffScreenCount;
    private Integer multipleFacesCount;
    private Integer faceMismatchCount;

    public void setAvgTechnicalAccuracy(double val) { this.averageTechnicalAccuracy = val; }
    public void setAvgCompleteness(double val) { this.averageCompleteness = val; }
    public void setAvgClarity(double val) { this.averageClarity = val; }
    public void setImprovementSuggestions(List<String> list) { this.suggestions = list; }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CourseItem {
        private String title;
        private String platform;
        private String description;
        private String url;
        private String tag;
    }
}
