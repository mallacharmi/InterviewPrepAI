package com.interviewprep.entity;

import com.interviewprep.entity.enums.Difficulty;
import com.interviewprep.entity.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "questions", indexes = {
        @Index(name = "idx_question_interview_id", columnList = "interview_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @Column(length = 4000)
    private String questionText;

    private String topic;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    private QuestionType questionType;

    private int sequenceNumber;

    @Column(length = 4000)
    private String expectedConcepts;

    @Builder.Default
    private boolean isFollowUp = false;

    private Long parentQuestionId;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL)
    private Answer answer;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
