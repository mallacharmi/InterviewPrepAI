package com.interviewprep.service;

import com.interviewprep.config.AIConfig;
import com.interviewprep.dto.ai.AIEvaluationRequest;
import com.interviewprep.dto.ai.AIEvaluationResponse;
import com.interviewprep.dto.ai.AIFollowUpRequest;
import com.interviewprep.dto.ai.AIFollowUpResponse;
import com.interviewprep.dto.request.AnswerSubmitRequest;
import com.interviewprep.dto.response.EvaluationResponse;
import com.interviewprep.entity.Answer;
import com.interviewprep.entity.Interview;
import com.interviewprep.entity.Question;
import com.interviewprep.entity.User;
import com.interviewprep.entity.enums.Difficulty;
import com.interviewprep.entity.enums.InterviewStatus;
import com.interviewprep.entity.enums.InterviewType;
import com.interviewprep.entity.enums.QuestionType;
import com.interviewprep.exception.InvalidInterviewStateException;
import com.interviewprep.exception.UnauthorizedAccessException;
import com.interviewprep.mapper.AnswerMapper;
import com.interviewprep.mapper.InterviewMapper;
import com.interviewprep.mapper.QuestionMapper;
import com.interviewprep.repository.AnswerRepository;
import com.interviewprep.repository.InterviewRepository;
import com.interviewprep.repository.QuestionRepository;
import com.interviewprep.service.ai.AIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private InterviewService interviewService;

    @Mock
    private AIService aiService;

    @Mock
    private AIConfig aiConfig;

    @Mock
    private InterviewMapper interviewMapper;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private AnswerMapper answerMapper;

    @InjectMocks
    private AnswerService answerService;

    private User user;
    private Interview interview;
    private Question question;
    private AnswerSubmitRequest request;
    private AIEvaluationResponse aiEvaluationResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        interview = new Interview();
        interview.setId(1L);
        interview.setUser(user);
        interview.setStatus(InterviewStatus.IN_PROGRESS);
        interview.setCompletedQuestions(0);
        interview.setTotalQuestions(5);
        interview.setInterviewType(InterviewType.TECHNICAL);
        interview.setDifficulty(Difficulty.MEDIUM);
        interview.setTopics("Java,Spring");

        question = new Question();
        question.setId(1L);
        question.setInterview(interview);
        question.setQuestionText("Test Question");
        question.setQuestionType(QuestionType.TECHNICAL);
        question.setTopic("Java");
        question.setSequenceNumber(1);

        request = new AnswerSubmitRequest();
        request.setAnswerText("My candidate answer text for evaluation.");

        aiEvaluationResponse = AIEvaluationResponse.builder()
                .score(8.0)
                .technicalAccuracy(8.0)
                .completeness(8.0)
                .clarity(8.0)
                .feedback("Good answer.")
                .missingConcepts(List.of())
                .improvementSuggestion("Keep it up.")
                .needsFollowUp(false)
                .build();
    }

    @Test
    void submitAnswer_Success() {
        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(answerRepository.existsByQuestionId(1L)).thenReturn(false);
        when(aiService.evaluateAnswer(any(AIEvaluationRequest.class))).thenReturn(aiEvaluationResponse);
        when(answerRepository.save(any(Answer.class))).thenAnswer(i -> i.getArgument(0));

        EvaluationResponse response = answerService.submitAnswer(1L, 1L, 1L, request);

        assertNotNull(response);
        assertEquals(8.0, response.getScore());
        assertEquals(1, interview.getCompletedQuestions());
        verify(answerRepository).save(any(Answer.class));
        verify(interviewRepository).save(any(Interview.class));
    }

    @Test
    void submitAnswer_DuplicateAnswer() {
        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(answerRepository.existsByQuestionId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> {
            answerService.submitAnswer(1L, 1L, 1L, request);
        });
    }

    @Test
    void submitAnswer_WrongInterview() {
        Interview otherInterview = new Interview();
        otherInterview.setId(2L);
        question.setInterview(otherInterview);

        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));

        assertThrows(IllegalArgumentException.class, () -> {
            answerService.submitAnswer(1L, 1L, 1L, request);
        });
    }

    @Test
    void submitAnswer_WithFollowUp() {
        aiEvaluationResponse.setNeedsFollowUp(true);

        AIFollowUpResponse followUpResponse = new AIFollowUpResponse("Follow up question text", "Java", List.of("Concurrency"));

        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(answerRepository.existsByQuestionId(1L)).thenReturn(false);
        when(aiService.evaluateAnswer(any(AIEvaluationRequest.class))).thenReturn(aiEvaluationResponse);
        when(questionRepository.countByInterviewIdAndParentQuestionId(1L, 1L)).thenReturn(0L);
        when(aiConfig.getMaxFollowUps()).thenReturn(1);
        when(aiService.generateFollowUp(any(AIFollowUpRequest.class))).thenReturn(followUpResponse);
        when(answerRepository.save(any(Answer.class))).thenAnswer(i -> i.getArgument(0));

        EvaluationResponse response = answerService.submitAnswer(1L, 1L, 1L, request);

        assertNotNull(response);
        assertTrue(response.isHasFollowUp());
        assertEquals("Follow up question text", response.getFollowUpQuestion());
        verify(aiService).generateFollowUp(any(AIFollowUpRequest.class));
    }
}
