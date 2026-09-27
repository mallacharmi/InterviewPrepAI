package com.interviewprep.repository;

import com.interviewprep.entity.Interview;
import com.interviewprep.entity.enums.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    Page<Interview> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Interview> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Interview> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
    
    long countByUserId(Long userId);
    
    long countByUserIdAndStatus(Long userId, InterviewStatus status);

    @Query("SELECT COUNT(i) FROM Interview i WHERE i.user.id = :userId AND (i.status = 'TERMINATED' OR i.terminationReason IS NOT NULL)")
    long countTerminatedByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(i.violationCount), 0) FROM Interview i WHERE i.user.id = :userId")
    long sumViolationsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT AVG(i.overallScore) FROM Interview i WHERE i.user.id = :userId AND i.overallScore IS NOT NULL")
    Double findAverageScoreByUserId(@Param("userId") Long userId);
    
    @Query("SELECT MAX(i.overallScore) FROM Interview i WHERE i.user.id = :userId AND i.overallScore IS NOT NULL")
    Double findMaxScoreByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(i.overallScore) FROM Interview i WHERE i.user.id = :userId AND i.overallScore IS NOT NULL")
    Double getAverageScoreByUserId(@Param("userId") Long userId);
    
    @Query("SELECT MAX(i.overallScore) FROM Interview i WHERE i.user.id = :userId AND i.overallScore IS NOT NULL")
    Double getMaxScoreByUserId(@Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM Interview i WHERE i.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
