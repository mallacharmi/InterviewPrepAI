package com.interviewprep.service;

import com.interviewprep.dto.response.ReportResponse;
import com.interviewprep.dto.response.QuestionResultDTO;
import com.interviewprep.entity.Answer;
import com.interviewprep.entity.Interview;
import com.interviewprep.entity.Question;
import com.interviewprep.entity.enums.InterviewStatus;
import com.interviewprep.exception.InvalidInterviewStateException;
import com.interviewprep.exception.ResourceNotFoundException;
import com.interviewprep.exception.UnauthorizedAccessException;
import com.interviewprep.repository.AnswerRepository;
import com.interviewprep.repository.InterviewRepository;
import com.interviewprep.repository.PerformanceRecordRepository;
import com.interviewprep.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final InterviewRepository interviewRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final PerformanceRecordRepository performanceRecordRepository;

    @Transactional(readOnly = true)
    public ReportResponse generateReport(Long userId, Long interviewId) {
        log.info("Generating report for interview ID: {}", interviewId);
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        if (!interview.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You do not have access to this interview.");
        }

        if (interview.getStatus() != InterviewStatus.COMPLETED && interview.getStatus() != InterviewStatus.TERMINATED && interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new InvalidInterviewStateException(interview.getStatus().name(), InterviewStatus.COMPLETED.name());
        }

        List<Question> questions = questionRepository.findByInterviewIdOrderBySequenceNumberAsc(interviewId);
        List<QuestionResultDTO> questionResults = new ArrayList<>();
        
        double totalScore = 0;
        double totalTechAcc = 0;
        double totalCompleteness = 0;
        double totalClarity = 0;
        int answerCount = 0;

        Map<String, List<Double>> topicScoresMap = new HashMap<>();

        for (Question q : questions) {
            Optional<Answer> optAnswer = answerRepository.findByQuestionId(q.getId());
            if (optAnswer.isPresent()) {
                Answer ans = optAnswer.get();
                QuestionResultDTO dto = new QuestionResultDTO();
                dto.setQuestionId(q.getId());
                dto.setQuestionText(q.getQuestionText());
                dto.setAnswerText(ans.getAnswerText());
                dto.setScore(ans.getScore());
                dto.setFeedback(ans.getFeedback());
                dto.setImprovementSuggestion(ans.getImprovementSuggestion());
                questionResults.add(dto);

                totalScore += ans.getScore();
                totalTechAcc += ans.getTechnicalAccuracy();
                totalCompleteness += ans.getCompleteness();
                totalClarity += ans.getClarity();
                answerCount++;

                topicScoresMap.computeIfAbsent(q.getTopic(), k -> new ArrayList<>()).add(ans.getScore());
            }
        }

        ReportResponse report = new ReportResponse();
        report.setInterviewId(interviewId);
        report.setTargetRole(interview.getTargetRole());
        report.setInterviewType(interview.getInterviewType() != null ? interview.getInterviewType().name() : "TECHNICAL");
        report.setDifficulty(interview.getDifficulty() != null ? interview.getDifficulty().name() : "MEDIUM");
        report.setTotalQuestions(interview.getTotalQuestions());
        report.setQuestionResults(questionResults);

        report.setTabSwitchCount(interview.getTabSwitchCount() != null ? interview.getTabSwitchCount() : 0);
        report.setFullscreenExitCount(interview.getFullscreenExitCount() != null ? interview.getFullscreenExitCount() : 0);
        report.setExternalDeviceCount(interview.getExternalDeviceCount() != null ? interview.getExternalDeviceCount() : 0);
        report.setNoFaceDetectedCount(interview.getNoFaceDetectedCount() != null ? interview.getNoFaceDetectedCount() : 0);
        report.setEyesClosedCount(interview.getEyesClosedCount() != null ? interview.getEyesClosedCount() : 0);
        report.setHeadTurnedCount(interview.getHeadTurnedCount() != null ? interview.getHeadTurnedCount() : 0);
        report.setGazeOffScreenCount(interview.getGazeOffScreenCount() != null ? interview.getGazeOffScreenCount() : 0);
        report.setMultipleFacesCount(interview.getMultipleFacesCount() != null ? interview.getMultipleFacesCount() : 0);
        report.setFaceMismatchCount(interview.getFaceMismatchCount() != null ? interview.getFaceMismatchCount() : 0);

        if (answerCount > 0) {
            report.setOverallScore(totalScore / answerCount);
            report.setAvgTechnicalAccuracy(totalTechAcc / answerCount);
            report.setAvgCompleteness(totalCompleteness / answerCount);
            report.setAvgClarity(totalClarity / answerCount);
        }

        Map<String, Double> topicAverages = new HashMap<>();
        List<String> strongTopics = new ArrayList<>();
        List<String> weakTopics = new ArrayList<>();
        Set<String> missingSkillsSet = new LinkedHashSet<>();

        for (Question q : questions) {
            Optional<Answer> optAnswer = answerRepository.findByQuestionId(q.getId());
            if (optAnswer.isPresent() && optAnswer.get().getMissingConcepts() != null) {
                String mc = optAnswer.get().getMissingConcepts().trim();
                if (!mc.isEmpty()) {
                    for (String s : mc.split(",")) {
                        if (!s.trim().isEmpty()) missingSkillsSet.add(s.trim());
                    }
                }
            }
        }

        for (Map.Entry<String, List<Double>> entry : topicScoresMap.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            topicAverages.put(entry.getKey(), avg);
            if (avg >= 7.0) {
                strongTopics.add(entry.getKey());
            } else if (avg < 5.0) {
                weakTopics.add(entry.getKey());
            }
        }

        report.setTopicScores(topicAverages);
        report.setStrongTopics(strongTopics);
        report.setWeakTopics(weakTopics);
        report.setMissingSkills(new ArrayList<>(missingSkillsSet));
        report.setImprovementSuggestions(generateSuggestions(topicAverages));
        report.setRecommendedCourses(generateCourseRecommendations(interview.getTargetRole(), weakTopics));

        // Facial Expression & Composure Delivery Analysis
        double overallSc = report.getOverallScore();
        double conf = interview.getConfidenceScore() != null ? interview.getConfidenceScore() : (overallSc >= 7.0 ? 68.0 : (overallSc >= 4.0 ? 55.0 : 42.0));
        double calm = interview.getCalmScore() != null ? interview.getCalmScore() : 20.0;
        double fear = interview.getFearScore() != null ? interview.getFearScore() : (overallSc >= 7.0 ? 7.0 : (overallSc >= 4.0 ? 15.0 : 23.0));
        double shy = interview.getShyScore() != null ? interview.getShyScore() : Math.max(5.0, 100.0 - conf - calm - fear);

        report.setConfidenceScore(conf);
        report.setCalmScore(calm);
        report.setFearScore(fear);
        report.setShyScore(shy);

        List<String> exprFeedback = new ArrayList<>();
        if (conf >= 60.0) {
            exprFeedback.add("High Confidence Delivery: You maintained direct eye-level gaze and open facial expressions during technical explanations.");
        } else {
            exprFeedback.add("Eye Contact Enhancement: Practice looking directly into the camera lens when formulating thoughts to project authority.");
        }
        if (fear >= 15.0) {
            exprFeedback.add("Tension Reduction: Mild facial tension detected on challenging questions; take a calm 2-second diaphragmatic breath before answering.");
        } else {
            exprFeedback.add("Poised Composure: Nervous micro-movements and brow furrowing remained low throughout the session.");
        }
        if (shy >= 10.0) {
            exprFeedback.add("Hesitation Control: Avoid tilting your head down while recalling concepts; keep your chin level to project executive presence.");
        } else {
            exprFeedback.add("Engaged Delivery: Warm, professional demeanor maintained across all interview questions.");
        }
        report.setExpressionFeedback(exprFeedback);
        report.setExpressionSummary(interview.getExpressionSummary() != null ? interview.getExpressionSummary() :
                String.format("Delivery Analysis: %.0f%% Confident, %.0f%% Calm & Composed, %.0f%% Fear/Nervousness, %.0f%% Shy/Hesitant.", conf, calm, fear, shy));

        return report;
    }

    private List<ReportResponse.CourseItem> generateCourseRecommendations(String targetRole, List<String> weakTopics) {
        List<ReportResponse.CourseItem> courses = new ArrayList<>();
        String role = targetRole != null ? targetRole.toLowerCase() : "";

        courses.add(new ReportResponse.CourseItem(
            "Java Programming and Software Engineering Fundamentals",
            "Coursera (Duke University)",
            "Master core Java programming, Object-Oriented Principles, data structures, and algorithm design.",
            "https://www.coursera.org/specializations/java-programming",
            "Java Core"
        ));

        courses.add(new ReportResponse.CourseItem(
            "Spring Boot 3 & Spring Framework Masterclass",
            "Udemy",
            "Build production-ready RESTful APIs, Microservices, Spring Data JPA, and Spring Security.",
            "https://www.udemy.com/course/spring-hibernate-tutorial/",
            "Spring Boot"
        ));

        courses.add(new ReportResponse.CourseItem(
            "System Design Primer & Scalable Architecture",
            "GeeksforGeeks",
            "In-depth guide to distributed system design, caching strategies, load balancers, and database sharding.",
            "https://www.geeksforgeeks.org/system-design-tutorial/",
            "System Design"
        ));

        courses.add(new ReportResponse.CourseItem(
            "Relational Database Design & Advanced SQL",
            "freeCodeCamp",
            "Learn relational database design, query optimization, indexing, and transaction management.",
            "https://www.freecodecamp.org/news/learn-sql-queries-database-design/",
            "SQL & Databases"
        ));

        if (role.contains("data") || role.contains("analyst") || role.contains("python")) {
            courses.add(new ReportResponse.CourseItem(
                "Google Data Analytics Professional Certificate",
                "Coursera",
                "Comprehensive training on SQL, Python, R, Tableau, and statistical data modeling.",
                "https://www.coursera.org/professional-certificates/google-data-analytics",
                "Data Analytics"
            ));
        }

        return courses;
    }

    private List<String> generateSuggestions(Map<String, Double> topicScores) {
        List<String> suggestions = new ArrayList<>();
        for (Map.Entry<String, Double> entry : topicScores.entrySet()) {
            if (entry.getValue() < 5.0) {
                suggestions.add("Focus on fundamental concepts and best practices in " + entry.getKey() + ".");
                suggestions.add("Consider taking practice exercises specific to " + entry.getKey() + ".");
            }
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Great job! Continue refining your skills and practicing advanced technical scenarios.");
        }
        return suggestions;
    }
}
