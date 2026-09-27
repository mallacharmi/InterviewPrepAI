package com.interviewprep.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProctoringAlertsRequest {
    private Integer tabSwitchCount;
    private Integer fullscreenExitCount;
    private Integer externalDeviceCount;
    private Integer noFaceDetectedCount;
    private Integer eyesClosedCount;
    private Integer headTurnedCount;
    private Integer gazeOffScreenCount;
    private Integer multipleFacesCount;
    private Integer faceMismatchCount;
}
