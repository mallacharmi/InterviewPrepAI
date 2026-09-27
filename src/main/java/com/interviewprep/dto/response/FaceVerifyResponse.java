package com.interviewprep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceVerifyResponse {

    private boolean matched;
    private double similarityScore; // 0.0 to 100.0 %
    private String message;
    private boolean faceDetected;
}
