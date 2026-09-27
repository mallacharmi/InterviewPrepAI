package com.interviewprep.dto.request;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewCreateRequest {
    @NotBlank
    private String targetRole;

    @NotNull
    private String interviewType;

    @NotNull
    private String difficulty;

    @Min(5)
    @Max(15)
    private int totalQuestions;

    private List<String> topics;
}
