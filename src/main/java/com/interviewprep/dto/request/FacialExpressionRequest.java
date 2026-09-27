package com.interviewprep.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacialExpressionRequest {
    private Double confidence;
    private Double fear;
    private Double shy;
    private Double calm;
    private String summary;
}
