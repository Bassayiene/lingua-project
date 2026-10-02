package com.lingua.learning.repository;

import com.lingua.learning.domain.LearnerScore;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LearnerScoreRepository extends JpaRepository<LearnerScore, Long> {
    Optional<LearnerScore> findByUserLogin(String userLogin);

    /**
     * Locks the row until the end of the transaction: every operation that changes the points of a
     * learner starts here, so two simultaneous requests of the same learner run one after the other.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from LearnerScore s where s.userLogin = :userLogin")
    Optional<LearnerScore> findForUpdate(String userLogin);
}
