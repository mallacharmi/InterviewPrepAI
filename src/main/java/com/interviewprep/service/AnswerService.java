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
import com.interviewprep.entity.enums.InterviewStatus;
import com.interviewprep.exception.InvalidInterviewStateException;
import com.interviewprep.exception.ResourceNotFoundException;
import com.interviewprep.exception.UnauthorizedAccessException;
import com.interviewprep.mapper.AnswerMapper;
import com.interviewprep.mapper.InterviewMapper;
import com.interviewprep.mapper.QuestionMapper;
import com.interviewprep.repository.AnswerRepository;
import com.interviewprep.repository.InterviewRepository;
import com.interviewprep.repository.QuestionRepository;
import com.interviewprep.service.ai.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewService interviewService;
    private final AIService aiService;
    private final AIConfig aiConfig;
    private final InterviewMapper interviewMapper;
    private final QuestionMapper questionMapper;
    private final AnswerMapper answerMapper;

    public EvaluationResponse submitAnswer(Long userId, Long interviewId, Long questionId, AnswerSubmitRequest request) {
        log.info("Submitting answer for question ID: {} in interview ID: {}", questionId, interviewId);
        
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        if (!interview.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You do not have access to this interview.");
        }

        if (interview.getStatus() == InterviewStatus.CREATED) {
            interview.setStatus(InterviewStatus.IN_PROGRESS);
            if (interview.getStartedAt() == null) {
                interview.setStartedAt(LocalDateTime.now());
            }
            interviewRepository.save(interview);
        } else if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new InvalidInterviewStateException(interview.getStatus().name(), InterviewStatus.IN_PROGRESS.name());
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        if (!question.getInterview().getId().equals(interviewId)) {
            throw new IllegalArgumentException("Question does not belong to this interview.");
        }

        if (answerRepository.existsByQuestionId(questionId)) {
            throw new IllegalStateException("An answer has already been submitted for this question.");
        }

        AIEvaluationRequest evaluationRequest = new AIEvaluationRequest();
        evaluationRequest.setQuestionText(question.getQuestionText());
        evaluationRequest.setAnswerText(request.getAnswerText());
        evaluationRequest.setTargetRole(interview.getTargetRole());
        evaluationRequest.setDifficulty(interview.getDifficulty().name());

        AIEvaluationResponse aiEvaluation = aiService.evaluateAnswer(evaluationRequest);

        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setUser(interview.getUser());
        answer.setAnswerText(request.getAnswerText());
        answer.setScore(aiEvaluation.getScore());
        answer.setTechnicalAccuracy(aiEvaluation.getTechnicalAccuracy());
        answer.setCompleteness(aiEvaluation.getCompleteness());
        answer.setClarity(aiEvaluation.getClarity());
        answer.setFeedback(aiEvaluation.getFeedback());
        answer.setMissingConcepts(aiEvaluation.getMissingConcepts() != null ? String.join(",", aiEvaluation.getMissingConcepts()) : "");
        answer.setImprovementSuggestion(aiEvaluation.getImprovementSuggestion());
        answer.setCreatedAt(LocalDateTime.now());
        
        answerRepository.save(answer);

        if (!question.isFollowUp()) {
            interview.setCompletedQuestions(interview.getCompletedQuestions() + 1);
        }

        boolean hasFollowUp = false;
        String followUpText = null;

        if (aiEvaluation.isNeedsFollowUp()) {
            long currentFollowUps = questionRepository.countByInterviewIdAndParentQuestionId(
                    interviewId, question.isFollowUp() ? question.getParentQuestionId() : question.getId());
            
            if (currentFollowUps < aiConfig.getMaxFollowUps()) {
                AIFollowUpRequest followUpRequest = new AIFollowUpRequest();
                followUpRequest.setOriginalQuestion(question.getQuestionText());
                followUpRequest.setCandidateAnswer(request.getAnswerText());
                followUpRequest.setFeedback(aiEvaluation.getFeedback());
                followUpRequest.setTopic(question.getTopic());
                followUpRequest.setWeakAreas(aiEvaluation.getMissingConcepts());
                
                AIFollowUpResponse followUpResponse = aiService.generateFollowUp(followUpRequest);
                
                Question followUpQ = new Question();
                followUpQ.setInterview(interview);
                followUpQ.setQuestionText(followUpResponse.getFollowUpQuestion());
                followUpQ.setQuestionType(com.interviewprep.entity.enums.QuestionType.FOLLOW_UP);
                followUpQ.setTopic(question.getTopic());
                followUpQ.setFollowUp(true);
                followUpQ.setParentQuestionId(question.isFollowUp() ? question.getParentQuestionId() : question.getId());
                long nextSeq = questionRepository.countByInterviewId(interviewId) + 1;
                followUpQ.setSequenceNumber((int) nextSeq);
                followUpQ.setCreatedAt(LocalDateTime.now());
                
                questionRepository.save(followUpQ);
                hasFollowUp = true;
                followUpText = followUpResponse.getFollowUpQuestion();
            }
        }

        if (!hasFollowUp && interview.getCompletedQuestions() < interview.getTotalQuestions()) {
            List<String> topics = List.of(interview.getTopics().split(","));
            long nextSeq = questionRepository.countByInterviewId(interviewId) + 1;
            interviewService.generateAndSaveQuestion(interview, topics, (int) nextSeq);
        }

        interviewRepository.save(interview);

        EvaluationResponse response = new EvaluationResponse();
        response.setQuestionId(question.getId());
        response.setQuestionText(question.getQuestionText());
        response.setAnswerText(answer.getAnswerText());
        response.setScore(answer.getScore());
        response.setTechnicalAccuracy(answer.getTechnicalAccuracy());
        response.setCompleteness(answer.getCompleteness());
        response.setClarity(answer.getClarity());
        response.setFeedback(answer.getFeedback());
        response.setMissingConcepts(aiEvaluation.getMissingConcepts());
        response.setImprovementSuggestion(answer.getImprovementSuggestion());
        response.setHasFollowUp(hasFollowUp);
        response.setFollowUpQuestion(followUpText);

        return response;
    }
}
