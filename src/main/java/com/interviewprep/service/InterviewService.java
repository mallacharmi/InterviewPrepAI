package com.interviewprep.service;

import com.interviewprep.config.AIConfig;
import com.interviewprep.dto.ai.AIQuestionRequest;
import com.interviewprep.dto.ai.AIQuestionResponse;
import com.interviewprep.dto.request.InterviewCreateRequest;
import com.interviewprep.dto.response.InterviewHistoryResponse;
import com.interviewprep.dto.response.InterviewResponse;
import com.interviewprep.dto.response.QuestionResponse;
import com.interviewprep.entity.Interview;
import com.interviewprep.entity.PerformanceRecord;
import com.interviewprep.entity.Question;
import com.interviewprep.entity.User;
import com.interviewprep.entity.enums.Difficulty;
import com.interviewprep.entity.enums.InterviewStatus;
import com.interviewprep.entity.enums.InterviewType;
import com.interviewprep.exception.InvalidInterviewStateException;
import com.interviewprep.exception.ResourceNotFoundException;
import com.interviewprep.exception.UnauthorizedAccessException;
import com.interviewprep.mapper.InterviewMapper;
import com.interviewprep.mapper.QuestionMapper;
import com.interviewprep.repository.AnswerRepository;
import com.interviewprep.repository.InterviewRepository;
import com.interviewprep.repository.PerformanceRecordRepository;
import com.interviewprep.repository.QuestionRepository;
import com.interviewprep.repository.UserRepository;
import com.interviewprep.service.ai.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final PerformanceRecordRepository performanceRecordRepository;
    private final UserRepository userRepository;
    private final AIService aiService;
    private final AIConfig aiConfig;
    private final InterviewMapper interviewMapper;
    private final QuestionMapper questionMapper;

    public InterviewResponse createInterview(Long userId, InterviewCreateRequest request) {
        log.info("Creating new interview for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Interview interview = new Interview();
        interview.setUser(user);
        interview.setTargetRole(request.getTargetRole());
        String typeStr = request.getInterviewType() != null ? request.getInterviewType().trim().toUpperCase() : "TECHNICAL";
        InterviewType parsedType;
        if (typeStr.equals("MIXED") || (typeStr.contains("HR") && (typeStr.contains("TECH") || typeStr.contains("+")))) {
            parsedType = InterviewType.MIXED;
        } else if (typeStr.equals("HR")) {
            parsedType = InterviewType.HR;
        } else {
            parsedType = InterviewType.TECHNICAL;
        }
        interview.setInterviewType(parsedType);
        interview.setDifficulty(Difficulty.valueOf(request.getDifficulty().toUpperCase()));
        interview.setTotalQuestions(request.getTotalQuestions());
        List<String> topicsList = request.getTopics();
        if (topicsList == null || topicsList.isEmpty()) {
            topicsList = List.of("Core Java", "OOP", "Problem Solving");
        }
        interview.setTopics(String.join(",", topicsList));
        interview.setStatus(InterviewStatus.CREATED);
        interview.setCreatedAt(LocalDateTime.now());

        Interview savedInterview = interviewRepository.save(interview);
        return interviewMapper.toResponse(savedInterview);
    }

    public QuestionResponse startInterview(Long userId, Long interviewId) {
        log.info("Starting interview ID: {} for user ID: {}", interviewId, userId);
        Interview interview = getAndValidateOwnership(userId, interviewId);

        if (interview.getStatus() != InterviewStatus.CREATED) {
            throw new InvalidInterviewStateException(interview.getStatus().name(), InterviewStatus.CREATED.name());
        }

        interview.setStatus(InterviewStatus.IN_PROGRESS);
        interview.setStartedAt(LocalDateTime.now());
        interviewRepository.save(interview);

        List<String> topics = List.of(interview.getTopics().split(","));
        Question firstQuestion = generateAndSaveQuestion(interview, topics, 1);

        return questionMapper.toResponse(firstQuestion);
    }

    @Transactional
    public QuestionResponse getCurrentQuestion(Long userId, Long interviewId) {
        Interview interview = getAndValidateOwnership(userId, interviewId);

        List<Question> questions = questionRepository.findByInterviewIdOrderBySequenceNumberAsc(interviewId);

        for (Question q : questions) {
            if (!answerRepository.existsByQuestionId(q.getId())) {
                return questionMapper.toResponse(q);
            }
        }

        if (interview.getCompletedQuestions() < interview.getTotalQuestions()) {
            List<String> topics = List.of(interview.getTopics().split(","));
            int nextSeq = questions.size() + 1;
            Question nextQuestion = generateAndSaveQuestion(interview, topics, nextSeq);
            return questionMapper.toResponse(nextQuestion);
        }

        if (interview.getStatus() != InterviewStatus.COMPLETED) {
            completeInterview(userId, interviewId);
        }

        throw new ResourceNotFoundException("Question", "interviewId", interviewId);
    }

    public void completeInterview(Long userId, Long interviewId) {
        log.info("Completing interview ID: {}", interviewId);
        Interview interview = getAndValidateOwnership(userId, interviewId);

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new InvalidInterviewStateException(interview.getStatus().name(), InterviewStatus.IN_PROGRESS.name());
        }

        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setCompletedAt(LocalDateTime.now());
        
        // Calculate overall score
        Double avgScore = interviewRepository.findAverageScoreByUserId(userId);
        interview.setOverallScore(avgScore != null ? avgScore : 0.0);
        
        interviewRepository.save(interview);
    }

    @Transactional
    public void terminateInterview(Long userId, Long interviewId, String reason, Integer violationCount) {
        log.info("Terminating interview ID: {} for user: {} due to: {}", interviewId, userId, reason);
        Interview interview = getAndValidateOwnership(userId, interviewId);

        interview.setStatus(InterviewStatus.TERMINATED);
        interview.setTerminationReason(reason != null ? reason : "Proctoring violation limit reached");
        interview.setViolationCount(violationCount != null ? violationCount : 3);
        interview.setCompletedAt(LocalDateTime.now());

        if (reason != null) {
            String lower = reason.toLowerCase();
            if (lower.contains("device") || lower.contains("mobile") || lower.contains("phone") || lower.contains("camera") || lower.contains("earphone")) {
                if (interview.getExternalDeviceCount() == null || interview.getExternalDeviceCount() == 0) {
                    interview.setExternalDeviceCount(1);
                }
            }
            if (lower.contains("multiple person") || lower.contains("multiple face")) {
                if (interview.getMultipleFacesCount() == null || interview.getMultipleFacesCount() == 0) {
                    interview.setMultipleFacesCount(1);
                }
            }
            if (lower.contains("tab") || lower.contains("focus")) {
                if (interview.getTabSwitchCount() == null || interview.getTabSwitchCount() == 0) {
                    interview.setTabSwitchCount(1);
                }
            }
            if (lower.contains("fullscreen")) {
                if (interview.getFullscreenExitCount() == null || interview.getFullscreenExitCount() == 0) {
                    interview.setFullscreenExitCount(1);
                }
            }
            if (lower.contains("no face")) {
                if (interview.getNoFaceDetectedCount() == null || interview.getNoFaceDetectedCount() == 0) {
                    interview.setNoFaceDetectedCount(1);
                }
            }
            if (lower.contains("head")) {
                if (interview.getHeadTurnedCount() == null || interview.getHeadTurnedCount() == 0) {
                    interview.setHeadTurnedCount(1);
                }
            }
            if (lower.contains("eyes")) {
                if (interview.getEyesClosedCount() == null || interview.getEyesClosedCount() == 0) {
                    interview.setEyesClosedCount(1);
                }
            }
            if (lower.contains("gaze") || lower.contains("off screen")) {
                if (interview.getGazeOffScreenCount() == null || interview.getGazeOffScreenCount() == 0) {
                    interview.setGazeOffScreenCount(1);
                }
            }
        }

        Double avgScore = interviewRepository.findAverageScoreByUserId(userId);
        interview.setOverallScore(avgScore != null ? avgScore : 0.0);

        interviewRepository.save(interview);
    }

    @Transactional
    public void updateProctoringAlerts(Long userId, Long interviewId, com.interviewprep.dto.request.ProctoringAlertsRequest request) {
        log.info("Updating proctoring alerts for interview ID: {} for user: {}", interviewId, userId);
        Interview interview = getAndValidateOwnership(userId, interviewId);

        if (request != null) {
            if (request.getTabSwitchCount() != null) interview.setTabSwitchCount(request.getTabSwitchCount());
            if (request.getFullscreenExitCount() != null) interview.setFullscreenExitCount(request.getFullscreenExitCount());
            if (request.getExternalDeviceCount() != null) interview.setExternalDeviceCount(request.getExternalDeviceCount());
            if (request.getNoFaceDetectedCount() != null) interview.setNoFaceDetectedCount(request.getNoFaceDetectedCount());
            if (request.getEyesClosedCount() != null) interview.setEyesClosedCount(request.getEyesClosedCount());
            if (request.getHeadTurnedCount() != null) interview.setHeadTurnedCount(request.getHeadTurnedCount());
            if (request.getGazeOffScreenCount() != null) interview.setGazeOffScreenCount(request.getGazeOffScreenCount());
            if (request.getMultipleFacesCount() != null) interview.setMultipleFacesCount(request.getMultipleFacesCount());
            if (request.getFaceMismatchCount() != null) interview.setFaceMismatchCount(request.getFaceMismatchCount());
            interviewRepository.save(interview);
        }
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> getUserInterviews(Long userId) {
        return interviewMapper.toResponseList(
                interviewRepository.findByUserIdOrderByCreatedAtDesc(
                        userId, PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent());
    }

    @Transactional(readOnly = true)
    public InterviewResponse getInterview(Long userId, Long interviewId) {
        Interview interview = getAndValidateOwnership(userId, interviewId);
        return interviewMapper.toResponse(interview);
    }

    @Transactional(readOnly = true)
    public InterviewHistoryResponse getInterviewHistory(Long userId, int page, int size) {
        Page<Interview> interviewPage = interviewRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        InterviewHistoryResponse response = new InterviewHistoryResponse();
        response.setContent(interviewMapper.toResponseList(interviewPage.getContent()));
        response.setCurrentPage(interviewPage.getNumber());
        response.setTotalPages(interviewPage.getTotalPages());
        response.setTotalElements(interviewPage.getTotalElements());
        response.setHasNext(interviewPage.hasNext());
        response.setHasPrevious(interviewPage.hasPrevious());
        return response;
    }

    @org.springframework.transaction.annotation.Transactional
    public void clearUserHistory(Long userId) {
        log.info("Clearing all interview history for user ID: {}", userId);
        List<Interview> userInterviews = interviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (!userInterviews.isEmpty()) {
            java.nio.file.Path recordingsDir = java.nio.file.Paths.get("data", "recordings").toAbsolutePath().normalize();
            for (Interview interview : userInterviews) {
                try {
                    java.nio.file.Path filePath = recordingsDir.resolve("interview_" + interview.getId() + ".webm");
                    java.nio.file.Files.deleteIfExists(filePath);
                } catch (Exception ignored) {}
            }
            interviewRepository.deleteAll(userInterviews);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public String saveInterviewRecording(Long userId, Long interviewId, org.springframework.web.multipart.MultipartFile file) {
        log.info("Saving video recording for interview ID: {}, size: {} bytes", interviewId, file.getSize());
        Interview interview = getAndValidateOwnership(userId, interviewId);
        try {
            java.nio.file.Path recordingsDir = java.nio.file.Paths.get("data", "recordings").toAbsolutePath().normalize();
            if (!java.nio.file.Files.exists(recordingsDir)) {
                java.nio.file.Files.createDirectories(recordingsDir);
            }
            java.nio.file.Path targetFile = recordingsDir.resolve("interview_" + interviewId + ".webm");
            try (java.io.InputStream inputStream = file.getInputStream()) {
                java.nio.file.Files.copy(inputStream, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            String videoUrl = "/api/interviews/" + interviewId + "/video";
            interview.setVideoRecordingUrl(videoUrl);
            interviewRepository.save(interview);
            return videoUrl;
        } catch (Exception e) {
            log.error("Failed to save recording for interview {}: {}", interviewId, e.getMessage());
            throw new RuntimeException("Failed to save recording: " + e.getMessage());
        }
    }

    public org.springframework.core.io.Resource getInterviewRecordingResource(Long userId, Long interviewId) {
        Interview interview = getAndValidateOwnership(userId, interviewId);
        java.nio.file.Path filePath = java.nio.file.Paths.get("data", "recordings", "interview_" + interviewId + ".webm").toAbsolutePath().normalize();
        if (!java.nio.file.Files.exists(filePath)) {
            throw new ResourceNotFoundException("VideoRecording", "interviewId", interviewId);
        }
        return new org.springframework.core.io.FileSystemResource(filePath.toFile());
    }

    public void cancelInterview(Long userId, Long interviewId) {
        log.info("Cancelling interview ID: {}", interviewId);
        Interview interview = getAndValidateOwnership(userId, interviewId);
        interview.setStatus(InterviewStatus.CANCELLED);
        interviewRepository.save(interview);
    }

    @Transactional
    public void deleteInterview(Long userId, Long interviewId) {
        log.info("Deleting interview ID: {} for user: {}", interviewId, userId);
        Interview interview = getAndValidateOwnership(userId, interviewId);

        // Delete associated video recording from filesystem
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get("data", "recordings", "interview_" + interviewId + ".webm").toAbsolutePath().normalize();
            java.nio.file.Files.deleteIfExists(filePath);
        } catch (Exception ignored) {}

        // Delete performance records referencing this interview
        try {
            performanceRecordRepository.deleteByInterviewId(interviewId);
        } catch (Exception ignored) {}

        interviewRepository.delete(interview);
    }

    @Transactional
    public int deleteInterviews(Long userId, List<Long> interviewIds) {
        if (interviewIds == null || interviewIds.isEmpty()) {
            return 0;
        }
        log.info("Batch deleting {} interviews for user: {}", interviewIds.size(), userId);
        int count = 0;
        for (Long id : interviewIds) {
            try {
                deleteInterview(userId, id);
                count++;
            } catch (Exception e) {
                log.warn("Failed to delete interview ID {}: {}", id, e.getMessage());
            }
        }
        return count;
    }

    private Interview getAndValidateOwnership(Long userId, Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));
        
        if (!interview.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You do not have access to this interview.");
        }
        return interview;
    }

    public Question generateAndSaveQuestion(Interview interview, List<String> topics, int sequenceNumber) {
        String selectedTopic = "General";
        InterviewType intType = interview.getInterviewType() != null ? interview.getInterviewType() : InterviewType.TECHNICAL;

        if (intType == InterviewType.HR) {
            selectedTopic = "Behavioral";
        } else if (intType == InterviewType.MIXED) {
            // Even questions (2, 4, 6...) are HR/Behavioral; odd questions (1, 3, 5...) are Technical
            if (sequenceNumber % 2 == 0) {
                selectedTopic = "Behavioral";
            } else if (topics != null && !topics.isEmpty()) {
                int idx = Math.abs((sequenceNumber - 1) / 2) % topics.size();
                selectedTopic = topics.get(idx).trim();
            }
        } else {
            if (topics != null && !topics.isEmpty()) {
                int idx = Math.abs(sequenceNumber - 1) % topics.size();
                selectedTopic = topics.get(idx).trim();
            }
        }
        
        AIQuestionRequest request = new AIQuestionRequest();
        request.setTargetRole(interview.getTargetRole());
        request.setInterviewType(interview.getInterviewType().name());
        request.setDifficulty(interview.getDifficulty().name());
        request.setTopic(selectedTopic);

        AIQuestionResponse aiResponse = aiService.generateQuestion(request);

        Question question = new Question();
        question.setInterview(interview);
        question.setQuestionText(aiResponse.getQuestionText());
        question.setQuestionType(aiResponse.getQuestionType() != null ? aiResponse.getQuestionType() : com.interviewprep.entity.enums.QuestionType.TECHNICAL);
        question.setDifficulty(interview.getDifficulty());
        question.setTopic(selectedTopic);
        question.setSequenceNumber(sequenceNumber);
        question.setCreatedAt(LocalDateTime.now());

        return questionRepository.save(question);
    }

    @Transactional
    public void saveFacialExpressions(Long userId, Long interviewId, com.interviewprep.dto.request.FacialExpressionRequest request) {
        log.info("Saving facial expressions for user ID: {}, interview ID: {}", userId, interviewId);
        Interview interview = getAndValidateOwnership(userId, interviewId);
        if (request != null) {
            if (request.getConfidence() != null) interview.setConfidenceScore(request.getConfidence());
            if (request.getFear() != null) interview.setFearScore(request.getFear());
            if (request.getShy() != null) interview.setShyScore(request.getShy());
            if (request.getCalm() != null) interview.setCalmScore(request.getCalm());
            if (request.getSummary() != null) interview.setExpressionSummary(request.getSummary());
            interviewRepository.save(interview);
            log.info("Saved facial expressions: confidence={}, fear={}, shy={}, calm={}", 
                    interview.getConfidenceScore(), interview.getFearScore(), interview.getShyScore(), interview.getCalmScore());
        }
    }
}
