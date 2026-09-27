package com.interviewprep.service.ai;

import com.interviewprep.dto.ai.AIEvaluationRequest;
import com.interviewprep.dto.ai.AIFollowUpRequest;
import com.interviewprep.dto.ai.AIQuestionRequest;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String buildQuestionGenerationPrompt(AIQuestionRequest request) {
        return """
               System: You are an expert technical interviewer. Generate interview questions in JSON format.
               User: Target Role: %s
               Topic: %s
               Difficulty: %s (HARD = deep conceptual, EASY = basic)
               Experience Level: %s
               Question Number: %d of %d
               
               REQUIRE ONLY valid JSON output with fields: question (String), topic (String), difficulty (String), expectedConcepts (array of String). No markdown, no explanation.
               """.formatted(
                request.getTargetRole(),
                request.getTopic(),
                request.getDifficulty(),
                request.getExperienceLevel(),
                request.getQuestionNumber(),
                request.getTotalQuestions()
        );
    }

    public String buildAnswerEvaluationPrompt(AIEvaluationRequest request) {
        return """
               System: You are an expert interview evaluator. Evaluate the candidate's answer.
               User: Question: %s
               Topic: %s
               Difficulty: %s
               Expected Concepts: %s
               Candidate Answer: %s
               
               REQUIRE ONLY valid JSON output with fields: score (number 0-10), technicalAccuracy (number 0-10), completeness (number 0-10), clarity (number 0-10), feedback (String), missingConcepts (array of String), improvementSuggestion (String), needsFollowUp (boolean). No markdown, no explanation.
               """.formatted(
                request.getQuestion(),
                request.getTopic(),
                request.getDifficulty(),
                String.join(", ", request.getExpectedConcepts()),
                request.getAnswer()
        );
    }

    public String buildFollowUpPrompt(AIFollowUpRequest request) {
        return """
               System: Generate a follow-up interview question based on the candidate's weak areas.
               User: Original Question: %s
               Candidate Answer: %s
               Topic: %s
               Difficulty: %s
               Weak Areas: %s
               
               REQUIRE ONLY valid JSON output with fields: question (String), topic (String), expectedConcepts (array of String). No markdown, no explanation.
               """.formatted(
                request.getOriginalQuestion(),
                request.getUserAnswer(),
                request.getTopic(),
                request.getDifficulty(),
                String.join(", ", request.getWeakAreas())
        );
    }
}
