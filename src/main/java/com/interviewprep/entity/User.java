package com.interviewprep.entity;

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

    private Boolean reminderEnabled;

    private String reminderTime;

    @Builder.Default
    private Integer currentStreak = 1;

    @Builder.Default
    private Integer maxStreak = 1;

    private java.time.LocalDate lastActiveDate;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String profilePhoto;

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
