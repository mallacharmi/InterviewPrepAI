package com.interviewprep.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "answers", indexes = {
        @Index(name = "idx_answer_question_id", columnList = "question_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", unique = true, nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 4000)
    private String answerText;

    private Double score;
    private Double technicalAccuracy;
    private Double completeness;
    private Double clarity;

    @Column(length = 4000)
    private String feedback;

    @Column(length = 4000)
    private String missingConcepts;

    @Column(length = 4000)
    private String improvementSuggestion;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
