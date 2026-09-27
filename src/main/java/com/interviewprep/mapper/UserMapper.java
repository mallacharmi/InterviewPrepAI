package com.interviewprep.mapper;
import com.interviewprep.dto.response.UserResponse;
import com.interviewprep.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .targetRole(user.getTargetRole())
                .experienceLevel(user.getExperienceLevel() != null ? user.getExperienceLevel().name() : null)
                .reminderEnabled(user.getReminderEnabled())
                .reminderTime(user.getReminderTime())
                .currentStreak(user.getCurrentStreak())
                .maxStreak(user.getMaxStreak())
                .profilePhoto(user.getProfilePhoto())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
