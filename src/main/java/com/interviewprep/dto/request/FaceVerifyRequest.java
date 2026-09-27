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
public class FaceVerifyRequest {

    @NotBlank(message = "Live camera snapshot image is required")
    private String liveImage;
}
