import os

base_dir = r"C:\Users\Charmi\.gemini\antigravity\scratch\ai-interview-prep\src\main\java\com\interviewprep"
res_dir = r"C:\Users\Charmi\.gemini\antigravity\scratch\ai-interview-prep\src\main\resources"

files = {
    f"{base_dir}\\entity\\enums\\InterviewStatus.java": """package com.interviewprep.entity.enums;

public enum InterviewStatus {
    CREATED, IN_PROGRESS, COMPLETED, CANCELLED
}
""",
    f"{base_dir}\\entity\\enums\\InterviewType.java": """package com.interviewprep.entity.enums;

public enum InterviewType {
    TECHNICAL, HR, MIXED
}
""",
    f"{base_dir}\\entity\\enums\\Difficulty.java": """package com.interviewprep.entity.enums;

public enum Difficulty {
    EASY, MEDIUM, HARD
}
""",
    f"{base_dir}\\entity\\enums\\ExperienceLevel.java": """package com.interviewprep.entity.enums;

public enum ExperienceLevel {
    FRESHER, JUNIOR, MID, SENIOR
}
""",
    f"{base_dir}\\entity\\enums\\QuestionType.java": """package com.interviewprep.entity.enums;

public enum QuestionType {
    TECHNICAL, BEHAVIORAL, SITUATIONAL, FOLLOW_UP
}
""",
    f"{base_dir}\\entity\\User.java": """package com.interviewprep.entity;

import com.interviewprep.entity.enums.ExperienceLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    private String targetRole;

    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    @ManyToMany
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
""",
    f"{base_dir}\\entity\\Role.java": """package com.interviewprep.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;
}
""",
    f"{base_dir}\\entity\\Interview.java": """package com.interviewprep.entity;

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

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
""",
    f"{base_dir}\\entity\\Question.java": """package com.interviewprep.entity;

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

    @Lob
    @Column(columnDefinition = "TEXT")
    private String questionText;

    private String topic;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    private QuestionType questionType;

    private int sequenceNumber;

    @Column(columnDefinition = "TEXT")
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
""",
    f"{base_dir}\\entity\\Answer.java": """package com.interviewprep.entity;

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

    @Lob
    @Column(columnDefinition = "TEXT")
    private String answerText;

    private Double score;
    private Double technicalAccuracy;
    private Double completeness;
    private Double clarity;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(columnDefinition = "TEXT")
    private String missingConcepts;

    @Column(columnDefinition = "TEXT")
    private String improvementSuggestion;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
""",
    f"{base_dir}\\entity\\PerformanceRecord.java": """package com.interviewprep.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "performance_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    private String topic;

    private Double score;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
""",
    f"{res_dir}\\db\\migration\\V1__create_users_and_roles.sql": """CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    target_role VARCHAR(255),
    experience_level VARCHAR(50),
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE INDEX idx_user_email (email)
);

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

INSERT INTO roles (name) VALUES ('ROLE_USER');
INSERT INTO roles (name) VALUES ('ROLE_ADMIN');
""",
    f"{res_dir}\\db\\migration\\V2__create_interviews.sql": """CREATE TABLE IF NOT EXISTS interviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_role VARCHAR(255),
    interview_type VARCHAR(50),
    difficulty VARCHAR(50),
    status VARCHAR(50) DEFAULT 'CREATED',
    total_questions INT NOT NULL,
    completed_questions INT DEFAULT 0,
    overall_score DOUBLE,
    started_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME,
    CONSTRAINT fk_interviews_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_interview_user_id (user_id),
    INDEX idx_interview_created_at (created_at)
);
""",
    f"{res_dir}\\db\\migration\\V3__create_questions.sql": """CREATE TABLE IF NOT EXISTS questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    interview_id BIGINT NOT NULL,
    question_text TEXT,
    topic VARCHAR(255),
    difficulty VARCHAR(50),
    question_type VARCHAR(50),
    sequence_number INT NOT NULL,
    expected_concepts TEXT,
    is_follow_up BOOLEAN DEFAULT FALSE,
    parent_question_id BIGINT,
    created_at DATETIME,
    CONSTRAINT fk_questions_interview FOREIGN KEY (interview_id) REFERENCES interviews (id),
    INDEX idx_question_interview_id (interview_id)
);
""",
    f"{res_dir}\\db\\migration\\V4__create_answers.sql": """CREATE TABLE IF NOT EXISTS answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    answer_text TEXT,
    score DOUBLE,
    technical_accuracy DOUBLE,
    completeness DOUBLE,
    clarity DOUBLE,
    feedback TEXT,
    missing_concepts TEXT,
    improvement_suggestion TEXT,
    created_at DATETIME,
    CONSTRAINT fk_answers_question FOREIGN KEY (question_id) REFERENCES questions (id),
    CONSTRAINT fk_answers_user FOREIGN KEY (user_id) REFERENCES users (id),
    UNIQUE INDEX idx_answer_question_id (question_id)
);
""",
    f"{res_dir}\\db\\migration\\V5__create_performance_records.sql": """CREATE TABLE IF NOT EXISTS performance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    interview_id BIGINT NOT NULL,
    topic VARCHAR(255),
    score DOUBLE,
    created_at DATETIME,
    CONSTRAINT fk_performance_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_performance_interview FOREIGN KEY (interview_id) REFERENCES interviews (id)
);
""",
    f"{base_dir}\\repository\\UserRepository.java": """package com.interviewprep.repository;

import com.interviewprep.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
""",
    f"{base_dir}\\repository\\RoleRepository.java": """package com.interviewprep.repository;

import com.interviewprep.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
""",
    f"{base_dir}\\repository\\InterviewRepository.java": """package com.interviewprep.repository;

import com.interviewprep.entity.Interview;
import com.interviewprep.entity.enums.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    Page<Interview> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Interview> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Interview> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
    
    long countByUserId(Long userId);
    
    long countByUserIdAndStatus(Long userId, InterviewStatus status);
    
    @Query("SELECT AVG(i.overallScore) FROM Interview i WHERE i.user.id = :userId AND i.overallScore IS NOT NULL")
    Double getAverageScoreByUserId(@Param("userId") Long userId);
    
    @Query("SELECT MAX(i.overallScore) FROM Interview i WHERE i.user.id = :userId AND i.overallScore IS NOT NULL")
    Double getMaxScoreByUserId(@Param("userId") Long userId);
}
""",
    f"{base_dir}\\repository\\QuestionRepository.java": """package com.interviewprep.repository;

import com.interviewprep.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByInterviewIdOrderBySequenceNumberAsc(Long interviewId);
    
    Optional<Question> findByInterviewIdAndSequenceNumber(Long interviewId, int sequenceNumber);
    
    long countByInterviewId(Long interviewId);
    
    long countByInterviewIdAndIsFollowUp(Long interviewId, boolean isFollowUp);
    
    List<Question> findByInterviewIdAndParentQuestionId(Long interviewId, Long parentQuestionId);
}
""",
    f"{base_dir}\\repository\\AnswerRepository.java": """package com.interviewprep.repository;

import com.interviewprep.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    Optional<Answer> findByQuestionId(Long questionId);
    
    List<Answer> findByUserId(Long userId);
    
    boolean existsByQuestionId(Long questionId);
}
""",
    f"{base_dir}\\repository\\PerformanceRecordRepository.java": """package com.interviewprep.repository;

import com.interviewprep.entity.PerformanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PerformanceRecordRepository extends JpaRepository<PerformanceRecord, Long> {
    List<PerformanceRecord> findByUserId(Long userId);
    
    List<PerformanceRecord> findByUserIdAndTopic(Long userId, String topic);
    
    @Query("SELECT p.topic, AVG(p.score) FROM PerformanceRecord p WHERE p.user.id = :userId GROUP BY p.topic")
    List<Object[]> getAverageScoreGroupedByTopic(@Param("userId") Long userId);
}
"""
}

for path, content in files.items():
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(content)

print("Files created successfully.")
