package com.interviewprep.repository;

import com.interviewprep.entity.Interview;
import com.interviewprep.entity.User;
import com.interviewprep.entity.enums.Difficulty;
import com.interviewprep.entity.enums.InterviewStatus;
import com.interviewprep.entity.enums.InterviewType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@ActiveProfiles("test")
class InterviewRepositoryTest {

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser = userRepository.save(testUser);

        Interview interview1 = new Interview();
        interview1.setUser(testUser);
        interview1.setStatus(InterviewStatus.COMPLETED);
        interview1.setInterviewType(InterviewType.TECHNICAL);
        interview1.setDifficulty(Difficulty.MEDIUM);
        interviewRepository.save(interview1);

        Interview interview2 = new Interview();
        interview2.setUser(testUser);
        interview2.setStatus(InterviewStatus.IN_PROGRESS);
        interview2.setInterviewType(InterviewType.HR);
        interview2.setDifficulty(Difficulty.EASY);
        interviewRepository.save(interview2);
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_ReturnsPaginated() {
        Page<Interview> page = interviewRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId(), PageRequest.of(0, 10));

        assertNotNull(page);
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void countByUserId_ReturnsCorrect() {
        long count = interviewRepository.countByUserId(testUser.getId());
        assertEquals(2, count);
    }

    @Test
    void countByUserIdAndStatus_ReturnsCorrect() {
        long completedCount = interviewRepository.countByUserIdAndStatus(testUser.getId(), InterviewStatus.COMPLETED);
        long inProgressCount = interviewRepository.countByUserIdAndStatus(testUser.getId(), InterviewStatus.IN_PROGRESS);
        long createdCount = interviewRepository.countByUserIdAndStatus(testUser.getId(), InterviewStatus.CREATED);

        assertEquals(1, completedCount);
        assertEquals(1, inProgressCount);
        assertEquals(0, createdCount);
    }
}
