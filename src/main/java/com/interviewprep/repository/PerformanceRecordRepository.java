package com.interviewprep.repository;

import com.interviewprep.entity.PerformanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PerformanceRecordRepository extends JpaRepository<PerformanceRecord, Long> {
    List<PerformanceRecord> findByUserId(Long userId);
    
    List<PerformanceRecord> findByUserIdAndTopic(Long userId, String topic);
    
    @Query("SELECT p.topic, AVG(p.score) FROM PerformanceRecord p WHERE p.user.id = :userId GROUP BY p.topic")
    List<Object[]> getAverageScoreGroupedByTopic(@Param("userId") Long userId);

    @Query("SELECT p.topic, AVG(p.score) FROM PerformanceRecord p WHERE p.user.id = :userId GROUP BY p.topic")
    List<Object[]> findAverageScoreByTopicForUser(@Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM PerformanceRecord p WHERE p.interview.id = :interviewId")
    void deleteByInterviewId(@Param("interviewId") Long interviewId);
}
