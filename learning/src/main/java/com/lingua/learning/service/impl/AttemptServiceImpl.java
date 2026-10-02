package com.lingua.learning.service.impl;

import static com.lingua.learning.repository.QuestionSpecifications.inCategory;
import static com.lingua.learning.repository.QuestionSpecifications.notYetSolvedBy;
import static com.lingua.learning.repository.QuestionSpecifications.withDifficulty;

import com.lingua.learning.config.ApplicationProperties;
import com.lingua.learning.domain.Choice;
import com.lingua.learning.domain.LearnerScore;
import com.lingua.learning.domain.Question;
import com.lingua.learning.domain.QuestionAttempt;
import com.lingua.learning.domain.enumeration.AttemptStatus;
import com.lingua.learning.domain.enumeration.Difficulty;
import com.lingua.learning.domain.enumeration.SoundEvent;
import com.lingua.learning.exception.BadRequestException;
import com.lingua.learning.exception.ConflictException;
import com.lingua.learning.exception.NotFoundException;
import com.lingua.learning.repository.LearnerScoreRepository;
import com.lingua.learning.repository.QuestionAttemptRepository;
import com.lingua.learning.repository.QuestionRepository;
import com.lingua.learning.service.AttemptService;
import com.lingua.learning.service.GameSettingsService;
import com.lingua.learning.service.dto.AnswerOutcome;
import com.lingua.learning.service.dto.AnswerResultDTO;
import com.lingua.learning.service.dto.StartedQuestionDTO;
import com.lingua.learning.service.mapper.QuestionMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for the timed attempts of learners.
 *
 * Every method starts by locking the score row of the learner, so simultaneous requests of the
 * same learner run one after the other and points cannot be credited or removed twice.
 */
@Service
@Transactional
public class AttemptServiceImpl implements AttemptService {

    private static final Logger LOG = LoggerFactory.getLogger(AttemptServiceImpl.class);

    private final QuestionAttemptRepository attemptRepository;

    private final QuestionRepository questionRepository;

    private final LearnerScoreRepository scoreRepository;

    private final GameSettingsService gameSettingsService;

    private final QuestionMapper questionMapper;

    private final Duration grace;

    private final Clock clock;

    public AttemptServiceImpl(
        QuestionAttemptRepository attemptRepository,
        QuestionRepository questionRepository,
        LearnerScoreRepository scoreRepository,
        GameSettingsService gameSettingsService,
        QuestionMapper questionMapper,
        ApplicationProperties properties,
        Clock clock
    ) {
        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
        this.scoreRepository = scoreRepository;
        this.gameSettingsService = gameSettingsService;
        this.questionMapper = questionMapper;
        this.grace = Duration.ofSeconds(properties.attempt().graceSeconds());
        this.clock = clock;
    }

    @Override
    public Optional<StartedQuestionDTO> next(String userLogin, Long categoryId, Difficulty difficulty) {
        Instant now = clock.instant();
        LearnerScore score = lockScore(userLogin, now);
        settleExpired(userLogin, score, now);

        // Asking again must not restart the timer or swap the question: hand back the running attempt
        List<QuestionAttempt> running = attemptRepository.findByUserLoginAndStatusOrderByIdAsc(userLogin, AttemptStatus.OPEN);
        if (!running.isEmpty()) {
            return Optional.of(toStartedQuestion(running.get(0), now));
        }

        Specification<Question> candidates = Specification.allOf(
            inCategory(categoryId),
            withDifficulty(difficulty),
            notYetSolvedBy(userLogin)
        );
        long count = questionRepository.count(candidates);
        if (count == 0) {
            return Optional.empty();
        }
        int index = ThreadLocalRandom.current().nextInt((int) Math.min(count, Integer.MAX_VALUE));
        List<Question> picked = questionRepository.findAll(candidates, PageRequest.of(index, 1, Sort.by("id"))).getContent();
        if (picked.isEmpty()) {
            // A question was deleted between the count and the read
            return Optional.empty();
        }
        Question question = picked.get(0);

        QuestionAttempt attempt = new QuestionAttempt();
        attempt.setUserLogin(userLogin);
        attempt.setQuestion(question);
        attempt.setStatus(AttemptStatus.OPEN);
        attempt.setStartedAt(now);
        attempt.setExpiresAt(now.plusSeconds(gameSettingsService.getDifficulty(question.getDifficulty()).timeLimitSeconds()));
        attempt = attemptRepository.save(attempt);
        LOG.debug("Attempt {} started for {} on question {}", attempt.getId(), userLogin, question.getId());
        return Optional.of(toStartedQuestion(attempt, now));
    }

    @Override
    public AnswerResultDTO answer(String userLogin, Long attemptId, Long choiceId) {
        Instant now = clock.instant();
        LearnerScore score = lockScore(userLogin, now);

        QuestionAttempt attempt = attemptRepository
            .findById(attemptId)
            // Same answer for an unknown attempt and for the attempt of someone else
            .filter(found -> found.getUserLogin().equals(userLogin))
            .orElseThrow(() -> new NotFoundException("Tentative introuvable : " + attemptId));

        if (attempt.getStatus() == AttemptStatus.EXPIRED) {
            // Already closed by settleExpired: repeat the outcome instead of an error
            return toResult(AnswerOutcome.TIME_EXPIRED, attempt, score);
        }
        if (attempt.getStatus() != AttemptStatus.OPEN) {
            throw new ConflictException("Cette tentative a déjà reçu une réponse");
        }
        if (isExpired(attempt, now)) {
            expire(attempt, score, now);
            return toResult(AnswerOutcome.TIME_EXPIRED, attempt, score);
        }

        Question question = attempt.getQuestion();
        Choice choice = question
            .getChoices()
            .stream()
            .filter(candidate -> candidate.getId().equals(choiceId))
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Ce choix n'appartient pas à la question"));

        attempt.setChoice(choice);
        attempt.setAnsweredAt(now);
        if (choice.isCorrect()) {
            // Points are only won the first time a question is answered correctly
            boolean alreadySolved = attemptRepository.existsByUserLoginAndQuestion_IdAndStatus(
                userLogin,
                question.getId(),
                AttemptStatus.CORRECT
            );
            attempt.setStatus(AttemptStatus.CORRECT);
            if (!alreadySolved) {
                int points = gameSettingsService.getDifficulty(question.getDifficulty()).points();
                attempt.setPointsDelta(points);
                score.setTotalPoints(score.getTotalPoints() + points);
                score.setCorrectAnswers(score.getCorrectAnswers() + 1);
                score.setUpdatedAt(now);
            }
            return toResult(AnswerOutcome.CORRECT, attempt, score);
        }
        attempt.setStatus(AttemptStatus.WRONG);
        attempt.setPointsDelta(-applyPenalty(score, question.getDifficulty(), now));
        return toResult(AnswerOutcome.WRONG, attempt, score);
    }

    @Override
    public void settleExpired(String userLogin) {
        Instant now = clock.instant();
        settleExpired(userLogin, lockScore(userLogin, now), now);
    }

    private void settleExpired(String userLogin, LearnerScore score, Instant now) {
        for (QuestionAttempt attempt : attemptRepository.findByUserLoginAndStatusOrderByIdAsc(userLogin, AttemptStatus.OPEN)) {
            if (isExpired(attempt, now)) {
                expire(attempt, score, now);
            }
        }
    }

    private boolean isExpired(QuestionAttempt attempt, Instant now) {
        return now.isAfter(attempt.getExpiresAt().plus(grace));
    }

    private void expire(QuestionAttempt attempt, LearnerScore score, Instant now) {
        attempt.setStatus(AttemptStatus.EXPIRED);
        attempt.setAnsweredAt(now);
        attempt.setPointsDelta(-applyPenalty(score, attempt.getQuestion().getDifficulty(), now));
        LOG.debug("Attempt {} of {} expired", attempt.getId(), attempt.getUserLogin());
    }

    /**
     * Remove the penalty of the level from the score, which never goes below zero.
     *
     * @return the points actually lost.
     */
    private int applyPenalty(LearnerScore score, Difficulty difficulty, Instant now) {
        int loss = Math.min(gameSettingsService.getDifficulty(difficulty).penaltyPoints(), score.getTotalPoints());
        if (loss > 0) {
            score.setTotalPoints(score.getTotalPoints() - loss);
            score.setUpdatedAt(now);
        }
        return loss;
    }

    private LearnerScore lockScore(String userLogin, Instant now) {
        return scoreRepository
            .findForUpdate(userLogin)
            .orElseGet(() -> {
                LearnerScore created = new LearnerScore();
                created.setUserLogin(userLogin);
                created.setUpdatedAt(now);
                return scoreRepository.saveAndFlush(created);
            });
    }

    private StartedQuestionDTO toStartedQuestion(QuestionAttempt attempt, Instant now) {
        long remaining = Math.max(0, Duration.between(now, attempt.getExpiresAt()).toSeconds());
        return new StartedQuestionDTO(
            attempt.getId(),
            questionMapper.toDto(attempt.getQuestion()),
            Duration.between(attempt.getStartedAt(), attempt.getExpiresAt()).toSeconds(),
            remaining,
            attempt.getStartedAt(),
            attempt.getExpiresAt()
        );
    }

    private AnswerResultDTO toResult(AnswerOutcome outcome, QuestionAttempt attempt, LearnerScore score) {
        Long correctChoiceId = attempt
            .getQuestion()
            .getChoices()
            .stream()
            .filter(Choice::isCorrect)
            .map(Choice::getId)
            .findFirst()
            .orElse(null);
        SoundEvent sound = outcome == AnswerOutcome.CORRECT ? SoundEvent.SUCCESS : SoundEvent.FAILURE;
        return new AnswerResultDTO(
            outcome,
            correctChoiceId,
            attempt.getPointsDelta(),
            score.getTotalPoints(),
            gameSettingsService.getSoundUrl(sound).orElse(null)
        );
    }
}
