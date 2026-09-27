package com.interviewprep.entity;

import com.interviewprep.entity.enums.Difficulty;
import com.interviewprep.entity.enums.InterviewStatus;
import com.interviewprep.entity.enums.InterviewType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "interviews", indexes = {
        @Index(name = "idx_interview_user_id", columnList = "user_id"),
        @Index(name = "idx_interview_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String targetRole;

    private String topics;

    @Enumerated(EnumType.STRING)
    private InterviewType interviewType;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InterviewStatus status = InterviewStatus.CREATED;

    private int totalQuestions;

    @Builder.Default
    private int completedQuestions = 0;

    private Double overallScore;

    private LocalDateTime startedAt;
    
    private LocalDateTime completedAt;

    @Column(name = "video_recording_url", length = 500)
    private String videoRecordingUrl;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "fear_score")
    private Double fearScore;

    @Column(name = "shy_score")
    private Double shyScore;

    @Column(name = "calm_score")
    private Double calmScore;

    @Column(name = "expression_summary", length = 1000)
    private String expressionSummary;

    @Column(name = "termination_reason")
    private String terminationReason;

    @Column(name = "violation_count")
    @Builder.Default
    private Integer violationCount = 0;

    @Column(name = "tab_switch_count")
    @Builder.Default
    private Integer tabSwitchCount = 0;

    @Column(name = "fullscreen_exit_count")
    @Builder.Default
    private Integer fullscreenExitCount = 0;

    @Column(name = "external_device_count")
    @Builder.Default
    private Integer externalDeviceCount = 0;

    @Column(name = "no_face_detected_count")
    @Builder.Default
    private Integer noFaceDetectedCount = 0;

    @Column(name = "eyes_closed_count")
    @Builder.Default
    private Integer eyesClosedCount = 0;

    @Column(name = "head_turned_count")
    @Builder.Default
    private Integer headTurnedCount = 0;

    @Column(name = "gaze_off_screen_count")
    @Builder.Default
    private Integer gazeOffScreenCount = 0;

    @Column(name = "multiple_faces_count")
    @Builder.Default
    private Integer multipleFacesCount = 0;

    @Column(name = "face_mismatch_count")
    @Builder.Default
    private Integer faceMismatchCount = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
