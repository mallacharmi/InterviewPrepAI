package com.interviewprep.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtsCreateInterviewRequest {
    private String resumeText;
    private String jobDescription;
    private String targetRole;
    private List<String> matchedKeywords;
    private List<String> missingSkills;
}
