package com.lingua.learning.repository;

import com.lingua.learning.domain.Question;
import com.lingua.learning.domain.QuestionAttempt;
import com.lingua.learning.domain.enumeration.AttemptStatus;
import com.lingua.learning.domain.enumeration.Difficulty;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filters on questions. A filter built from a null value is null, which means "no restriction".
 */
public final class QuestionSpecifications {

    private QuestionSpecifications() {}

    public static Specification<Question> inCategory(Long categoryId) {
        return categoryId == null ? null : (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Question> withDifficulty(Difficulty difficulty) {
        return difficulty == null ? null : (root, query, cb) -> cb.equal(root.get("difficulty"), difficulty);
    }

    /**
     * Questions the learner has not answered correctly yet.
     */
    public static Specification<Question> notYetSolvedBy(String userLogin) {
        return (root, query, cb) -> {
            Subquery<Long> solved = query.subquery(Long.class);
            Root<QuestionAttempt> attempt = solved.from(QuestionAttempt.class);
            solved
                .select(attempt.get("id"))
                .where(
                    cb.equal(attempt.get("question"), root),
                    cb.equal(attempt.get("userLogin"), userLogin),
                    cb.equal(attempt.get("status"), AttemptStatus.CORRECT)
                );
            return cb.not(cb.exists(solved));
        };
    }
}
