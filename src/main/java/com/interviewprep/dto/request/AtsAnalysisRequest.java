package com.interviewprep.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtsAnalysisRequest {
    @NotBlank(message = "Resume content is required")
    private String resumeText;

    @NotBlank(message = "Job Description content is required")
    private String jobDescription;
}
