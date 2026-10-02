package com.lingua.learning.service;

import com.lingua.learning.domain.enumeration.Difficulty;
import com.lingua.learning.service.dto.QuestionAdminDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing questions and their choices (admin side).
 * Learners get their questions through AttemptService, which starts the timer.
 */
public interface QuestionService {
    /**
     * @param categoryId optional filter.
     * @param difficulty optional filter.
     */
    Page<QuestionAdminDTO> findAll(Long categoryId, Difficulty difficulty, Pageable pageable);

    QuestionAdminDTO findOne(Long id);

    QuestionAdminDTO create(QuestionAdminDTO question);

    /**
     * Replaces the question and all its choices.
     */
    QuestionAdminDTO update(Long id, QuestionAdminDTO question);

    void delete(Long id);
}
