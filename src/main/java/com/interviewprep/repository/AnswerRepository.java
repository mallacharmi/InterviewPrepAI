package com.interviewprep.repository;

import com.interviewprep.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    Optional<Answer> findByQuestionId(Long questionId);
    
    List<Answer> findByUserId(Long userId);

    long countByUserId(Long userId);
    
    boolean existsByQuestionId(Long questionId);
}
