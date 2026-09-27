package com.interviewprep.service;

import com.interviewprep.dto.response.ReportResponse;
import com.interviewprep.entity.Answer;
import com.interviewprep.entity.Interview;
import com.interviewprep.entity.Question;
import com.interviewprep.entity.User;
import com.interviewprep.entity.enums.InterviewStatus;
import com.interviewprep.exception.InvalidInterviewStateException;
import com.interviewprep.exception.UnauthorizedAccessException;
import com.interviewprep.repository.AnswerRepository;
import com.interviewprep.repository.InterviewRepository;
import com.interviewprep.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @InjectMocks
    private ReportService reportService;

    private User user;
    private Interview interview;
    private Question q1;
    private Question q2;
    private Answer a1;
    private Answer a2;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        interview = new Interview();
        interview.setId(1L);
        interview.setUser(user);
        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setTotalQuestions(2);
        interview.setCompletedQuestions(2);
        interview.setOverallScore(8.5);
        interview.setStartedAt(LocalDateTime.now().minusMinutes(30));
        interview.setCompletedAt(LocalDateTime.now());
        
        q1 = new Question();
        q1.setId(1L);
        q1.setQuestionText("Question 1");
        q1.setTopic("Java");

        a1 = new Answer();
        a1.setQuestion(q1);
        a1.setAnswerText("Answer 1");
        a1.setScore(9.0);
        a1.setTechnicalAccuracy(9.0);
        a1.setCompleteness(9.0);
        a1.setClarity(9.0);

        q2 = new Question();
        q2.setId(2L);
        q2.setQuestionText("Question 2");
        q2.setTopic("Spring");

        a2 = new Answer();
        a2.setQuestion(q2);
        a2.setAnswerText("Answer 2");
        a2.setScore(8.0);
        a2.setTechnicalAccuracy(8.0);
        a2.setCompleteness(8.0);
        a2.setClarity(8.0);
    }

    @Test
    void generateReport_Success() {
        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));
        when(questionRepository.findByInterviewIdOrderBySequenceNumberAsc(1L)).thenReturn(List.of(q1, q2));
        when(answerRepository.findByQuestionId(1L)).thenReturn(Optional.of(a1));
        when(answerRepository.findByQuestionId(2L)).thenReturn(Optional.of(a2));

        ReportResponse report = reportService.generateReport(1L, 1L);

        assertNotNull(report);
        assertEquals(8.5, report.getOverallScore());
        assertEquals(2, report.getQuestionResults().size());
        assertTrue(report.getTopicScores().containsKey("Java"));
        assertTrue(report.getTopicScores().containsKey("Spring"));
    }

    @Test
    void generateReport_NotCompleted() {
        interview.setStatus(InterviewStatus.IN_PROGRESS);
        
        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));

        assertThrows(InvalidInterviewStateException.class, () -> {
            reportService.generateReport(1L, 1L);
        });
    }

    @Test
    void generateReport_UnauthorizedAccess() {
        User otherUser = new User();
        otherUser.setId(2L);
        interview.setUser(otherUser);

        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));

        assertThrows(UnauthorizedAccessException.class, () -> {
            reportService.generateReport(1L, 1L);
        });
    }
}
