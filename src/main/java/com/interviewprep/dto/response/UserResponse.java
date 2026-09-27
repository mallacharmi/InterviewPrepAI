package com.interviewprep.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String targetRole;
    private String experienceLevel;
    private Boolean reminderEnabled;
    private String reminderTime;
    private Integer currentStreak;
    private Integer maxStreak;
    private String profilePhoto;
    private LocalDateTime createdAt;
}
