package com.interviewprep.service;

import com.interviewprep.config.AIConfig;
import com.interviewprep.dto.ai.AIQuestionRequest;
import com.interviewprep.dto.ai.AIQuestionResponse;
import com.interviewprep.dto.request.InterviewCreateRequest;
import com.interviewprep.dto.response.InterviewHistoryResponse;
import com.interviewprep.dto.response.InterviewResponse;
import com.interviewprep.dto.response.QuestionResponse;
import com.interviewprep.entity.Interview;
import com.interviewprep.entity.Question;
import com.interviewprep.entity.User;
import com.interviewprep.entity.enums.Difficulty;
import com.interviewprep.entity.enums.InterviewStatus;
import com.interviewprep.entity.enums.InterviewType;
import com.interviewprep.exception.InvalidInterviewStateException;
import com.interviewprep.exception.UnauthorizedAccessException;
import com.interviewprep.mapper.InterviewMapper;
import com.interviewprep.mapper.QuestionMapper;
import com.interviewprep.repository.AnswerRepository;
import com.interviewprep.repository.InterviewRepository;
import com.interviewprep.repository.QuestionRepository;
import com.interviewprep.repository.UserRepository;
import com.interviewprep.service.ai.AIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AIService aiService;

    @Mock
    private AIConfig aiConfig;

    @Mock
    private InterviewMapper interviewMapper;

    @Mock
    private QuestionMapper questionMapper;

    @InjectMocks
    private InterviewService interviewService;

    private User user;
    private Interview interview;
    private Question question;
    private InterviewCreateRequest createRequest;
    private InterviewResponse interviewResponse;
    private QuestionResponse questionResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        interview = new Interview();
        interview.setId(1L);
        interview.setUser(user);
        interview.setStatus(InterviewStatus.CREATED);
        interview.setInterviewType(InterviewType.TECHNICAL);
        interview.setDifficulty(Difficulty.MEDIUM);
        interview.setTotalQuestions(5);
        interview.setCompletedQuestions(0);
        interview.setTopics("Java,Spring");

        question = new Question();
        question.setId(1L);
        question.setInterview(interview);
        question.setQuestionText("Test Question");
        question.setSequenceNumber(1);

        createRequest = new InterviewCreateRequest();
        createRequest.setTargetRole("Java Developer");
        createRequest.setInterviewType("TECHNICAL");
        createRequest.setDifficulty("MEDIUM");
        createRequest.setTotalQuestions(5);
        createRequest.setTopics(List.of("Java", "Spring"));

        interviewResponse = InterviewResponse.builder()
                .id(1L)
                .targetRole("Java Developer")
                .interviewType("TECHNICAL")
                .difficulty("MEDIUM")
                .status("CREATED")
                .totalQuestions(5)
                .completedQuestions(0)
                .build();

        questionResponse = QuestionResponse.builder()
                .id(1L)
                .questionText("Test Question")
                .sequenceNumber(1)
                .build();
    }

    @Test
    void createInterview_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(interviewRepository.save(any(Interview.class))).thenReturn(interview);
        when(interviewMapper.toResponse(any(Interview.class))).thenReturn(interviewResponse);

        InterviewResponse response = interviewService.createInterview(1L, createRequest);

        assertNotNull(response);
        assertEquals("CREATED", response.getStatus());
        verify(interviewRepository).save(any(Interview.class));
    }

    @Test
    void startInterview_Success() {
        AIQuestionResponse aiQuestionResponse = new AIQuestionResponse("Test Question", "Java", "MEDIUM", List.of("OOP"));
        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));
        when(aiService.generateQuestion(any(AIQuestionRequest.class))).thenReturn(aiQuestionResponse);
        when(questionRepository.save(any(Question.class))).thenReturn(question);
        when(questionMapper.toResponse(any(Question.class))).thenReturn(questionResponse);

        QuestionResponse response = interviewService.startInterview(1L, 1L);

        assertNotNull(response);
        assertEquals(InterviewStatus.IN_PROGRESS, interview.getStatus());
        verify(aiService).generateQuestion(any(AIQuestionRequest.class));
        verify(questionRepository).save(any(Question.class));
    }

    @Test
    void startInterview_WrongUser() {
        User otherUser = new User();
        otherUser.setId(2L);
        interview.setUser(otherUser);

        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));

        assertThrows(UnauthorizedAccessException.class, () -> {
            interviewService.startInterview(1L, 1L);
        });
    }

    @Test
    void startInterview_InvalidState() {
        interview.setStatus(InterviewStatus.IN_PROGRESS);

        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));

        assertThrows(InvalidInterviewStateException.class, () -> {
            interviewService.startInterview(1L, 1L);
        });
    }

    @Test
    void getCurrentQuestion_Success() {
        interview.setStatus(InterviewStatus.IN_PROGRESS);
        interview.setCompletedQuestions(0);
        
        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));
        when(questionRepository.findByInterviewIdOrderBySequenceNumberAsc(1L)).thenReturn(List.of(question));
        when(answerRepository.existsByQuestionId(question.getId())).thenReturn(false);
        when(questionMapper.toResponse(question)).thenReturn(questionResponse);

        QuestionResponse response = interviewService.getCurrentQuestion(1L, 1L);

        assertNotNull(response);
        assertEquals(question.getId(), response.getId());
    }

    @Test
    void completeInterview_Success() {
        interview.setStatus(InterviewStatus.IN_PROGRESS);
        
        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(any(Interview.class))).thenReturn(interview);

        interviewService.completeInterview(1L, 1L);

        assertEquals(InterviewStatus.COMPLETED, interview.getStatus());
        assertNotNull(interview.getCompletedAt());
        verify(interviewRepository).save(any(Interview.class));
    }

    @Test
    void completeInterview_InvalidState() {
        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));

        assertThrows(InvalidInterviewStateException.class, () -> {
            interviewService.completeInterview(1L, 1L);
        });
    }

    @Test
    void getInterviewHistory_Paginated() {
        Page<Interview> page = new PageImpl<>(Collections.singletonList(interview));
        when(interviewRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class))).thenReturn(page);
        when(interviewMapper.toResponseList(any())).thenReturn(List.of(interviewResponse));

        InterviewHistoryResponse result = interviewService.getInterviewHistory(1L, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
