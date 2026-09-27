package com.interviewprep.repository;

import com.interviewprep.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByInterviewIdOrderBySequenceNumberAsc(Long interviewId);
    
    Optional<Question> findByInterviewIdAndSequenceNumber(Long interviewId, int sequenceNumber);
    
    long countByInterviewId(Long interviewId);
    
    long countByInterviewIdAndIsFollowUp(Long interviewId, boolean isFollowUp);
    
    List<Question> findByInterviewIdAndParentQuestionId(Long interviewId, Long parentQuestionId);

    long countByInterviewIdAndParentQuestionId(Long interviewId, Long parentQuestionId);
}
