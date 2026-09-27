package com.interviewprep.service;

import com.interviewprep.dto.response.DashboardResponse;
import com.interviewprep.dto.response.InterviewResponse;
import com.interviewprep.entity.Interview;
import com.interviewprep.entity.enums.InterviewStatus;
import com.interviewprep.mapper.InterviewMapper;
import com.interviewprep.repository.InterviewRepository;
import com.interviewprep.repository.PerformanceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private final InterviewRepository interviewRepository;
    private final PerformanceRecordRepository performanceRecordRepository;
    private final com.interviewprep.repository.AnswerRepository answerRepository;
    private final com.interviewprep.repository.UserRepository userRepository;
    private final InterviewMapper interviewMapper;

    @Transactional
    public DashboardResponse getDashboard(Long userId) {
        log.info("Fetching dashboard for user ID: {}", userId);
        
        DashboardResponse response = new DashboardResponse();

        // 1. Streak Tracking (Daily logging & practice)
        int currentStreak = 1;
        int maxStreak = 1;
        com.interviewprep.entity.User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate lastActive = user.getLastActiveDate();
            if (lastActive == null) {
                user.setCurrentStreak(1);
                user.setMaxStreak(Math.max(1, user.getMaxStreak() != null ? user.getMaxStreak() : 1));
                user.setLastActiveDate(today);
                userRepository.save(user);
            } else if (!lastActive.equals(today)) {
                if (lastActive.equals(today.minusDays(1))) {
                    int newStreak = (user.getCurrentStreak() != null ? user.getCurrentStreak() : 0) + 1;
                    user.setCurrentStreak(newStreak);
                    user.setMaxStreak(Math.max(newStreak, user.getMaxStreak() != null ? user.getMaxStreak() : 1));
                } else {
                    user.setCurrentStreak(1);
                }
                user.setLastActiveDate(today);
                userRepository.save(user);
            }
            currentStreak = user.getCurrentStreak() != null ? user.getCurrentStreak() : 1;
            maxStreak = user.getMaxStreak() != null ? user.getMaxStreak() : 1;
        }

        long totalInterviews = interviewRepository.countByUserId(userId);
        long completedInterviews = interviewRepository.countByUserIdAndStatus(userId, InterviewStatus.COMPLETED);
        long terminatedInterviews = interviewRepository.countTerminatedByUserId(userId);
        long totalViolations = interviewRepository.sumViolationsByUserId(userId);
        long totalQuestionsAnswered = answerRepository.countByUserId(userId);
        
        Double avgScore = interviewRepository.findAverageScoreByUserId(userId);
        Double highestScore = interviewRepository.findMaxScoreByUserId(userId);
        
        response.setTotalInterviews(totalInterviews);
        response.setCompletedInterviews(completedInterviews);
        response.setTerminatedInterviews(terminatedInterviews);
        response.setTotalViolations(totalViolations);
        response.setTotalQuestionsAnswered(totalQuestionsAnswered);
        response.setCurrentStreak(currentStreak);
        response.setMaxStreak(maxStreak);
        response.setAverageScore(avgScore != null ? avgScore : 0.0);
        response.setHighestScore(highestScore != null ? highestScore : 0.0);
        
        List<Interview> recentInterviews = interviewRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId);
        response.setRecentInterviews(interviewMapper.toResponseList(recentInterviews));
        
        List<Object[]> topicAverages = performanceRecordRepository.findAverageScoreByTopicForUser(userId);
        Map<String, Double> topicPerformance = new HashMap<>();
        
        if (topicAverages != null) {
            for (Object[] record : topicAverages) {
                String topic = (String) record[0];
                Double score = (Double) record[1];
                topicPerformance.put(topic, score);
            }
        }
        
        response.setTopicPerformance(topicPerformance);
        
        List<String> strongestTopics = topicPerformance.entrySet().stream()
                .filter(e -> e.getValue() >= 7.0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
                
        List<String> weakestTopics = topicPerformance.entrySet().stream()
                .filter(e -> e.getValue() < 5.0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
                
        response.setStrongestTopics(strongestTopics);
        response.setWeakestTopics(weakestTopics);
        
        return response;
    }
}
