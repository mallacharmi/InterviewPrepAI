package com.interviewprep.service.ai;

import com.interviewprep.dto.ai.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockAIServiceTest {

    private MockAIService mockAIService;

    @BeforeEach
    void setUp() {
        mockAIService = new MockAIService();
    }

    @Test
    void generateQuestion_ReturnsValidQuestion() {
        AIQuestionRequest request = new AIQuestionRequest();
        request.setTargetRole("Java Developer");
        request.setInterviewType("TECHNICAL");
        request.setDifficulty("MEDIUM");
        request.setTopic("OOP");

        AIQuestionResponse response = mockAIService.generateQuestion(request);
        
        assertNotNull(response);
        assertNotNull(response.getQuestionText());
        assertFalse(response.getQuestionText().isBlank());
        assertEquals("OOP", response.getTopic());
    }

    @Test
    void evaluateAnswer_ReturnsValidEvaluation() {
        AIEvaluationRequest request = new AIEvaluationRequest();
        request.setQuestionText("Explain polymorphism.");
        request.setAnswerText("Polymorphism allows objects to take many forms.");
        request.setTargetRole("Java Developer");
        request.setDifficulty("MEDIUM");

        AIEvaluationResponse response = mockAIService.evaluateAnswer(request);
        
        assertNotNull(response);
        assertTrue(response.getScore() >= 0 && response.getScore() <= 10);
        assertTrue(response.getTechnicalAccuracy() >= 0 && response.getTechnicalAccuracy() <= 10);
        assertTrue(response.getClarity() >= 0 && response.getClarity() <= 10);
        assertTrue(response.getCompleteness() >= 0 && response.getCompleteness() <= 10);
        assertNotNull(response.getFeedback());
    }

    @Test
    void generateFollowUp_ReturnsValidFollowUp() {
        AIFollowUpRequest request = new AIFollowUpRequest();
        request.setOriginalQuestion("Explain polymorphism.");
        request.setCandidateAnswer("Polymorphism allows objects to take many forms.");
        request.setTopic("OOP");
        request.setWeakAreas(List.of("Method overriding"));

        AIFollowUpResponse response = mockAIService.generateFollowUp(request);
        
        assertNotNull(response);
        assertNotNull(response.getFollowUpQuestion());
        assertFalse(response.getFollowUpQuestion().isBlank());
    }

    @Test
    void isAvailable_ReturnsTrue() {
        assertTrue(mockAIService.isAvailable());
    }
}
