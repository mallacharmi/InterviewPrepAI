package com.interviewprep.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {
    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    private String targetRole;
    private String experienceLevel;
    private Boolean reminderEnabled;
    private String reminderTime;
    private String profilePhoto;
}
