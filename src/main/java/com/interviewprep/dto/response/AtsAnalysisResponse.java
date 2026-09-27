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
public class AtsAnalysisResponse {
    private double atsScore;
    private String matchCategory;
    private String targetRole;
    private List<String> matchedKeywords;
    private List<String> missingSkills;
    private List<String> improvementSuggestions;
    private List<ReportResponse.CourseItem> courseRecommendations;
}
