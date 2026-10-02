package com.lingua.learning.repository;

import com.lingua.learning.domain.QuestionAttempt;
import com.lingua.learning.domain.enumeration.AttemptStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, Long> {
    List<QuestionAttempt> findByUserLoginAndStatusOrderByIdAsc(String userLogin, AttemptStatus status);

    boolean existsByUserLoginAndQuestion_IdAndStatus(String userLogin, Long questionId, AttemptStatus status);
}
