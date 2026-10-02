package com.lingua.learning.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lingua.learning.config.ApplicationProperties;
import com.lingua.learning.domain.Category;
import com.lingua.learning.domain.Choice;
import com.lingua.learning.domain.LearnerScore;
import com.lingua.learning.domain.Question;
import com.lingua.learning.domain.QuestionAttempt;
import com.lingua.learning.domain.enumeration.AttemptStatus;
import com.lingua.learning.domain.enumeration.ChoiceType;
import com.lingua.learning.domain.enumeration.Difficulty;
import com.lingua.learning.domain.enumeration.SoundEvent;
import com.lingua.learning.exception.BadRequestException;
import com.lingua.learning.exception.ConflictException;
import com.lingua.learning.exception.NotFoundException;
import com.lingua.learning.repository.LearnerScoreRepository;
import com.lingua.learning.repository.QuestionAttemptRepository;
import com.lingua.learning.repository.QuestionRepository;
import com.lingua.learning.service.GameSettingsService;
import com.lingua.learning.service.dto.AnswerOutcome;
import com.lingua.learning.service.dto.AnswerResultDTO;
import com.lingua.learning.service.dto.DifficultySettingDTO;
import com.lingua.learning.service.dto.StartedQuestionDTO;
import com.lingua.learning.service.mapper.QuestionMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttemptServiceImplTest {

    private static final String LOGIN = "alice";
    private static final Instant START = Instant.parse("2026-01-01T10:00:00Z");

    private static final int POINTS = 10;
    private static final int PENALTY = 5;
    private static final int TIME_LIMIT = 30;
    private static final long GRACE = 2;

    private static final long CORRECT_CHOICE = 10L;
    private static final long WRONG_CHOICE = 11L;

    private static final String SUCCESS_SOUND = "http://media/success.mp3";
    private static final String FAILURE_SOUND = "http://media/failure.mp3";

    @Mock
    private QuestionAttemptRepository attemptRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private LearnerScoreRepository scoreRepository;

    @Mock
    private GameSettingsService gameSettingsService;

    private final MutableClock clock = new MutableClock(START);

    /** Stands for the question_attempt table. */
    private final List<QuestionAttempt> attempts = new ArrayList<>();

    private LearnerScore score;

    private Question question;

    private AttemptServiceImpl service;

    @BeforeEach
    void setUp() {
        score = new LearnerScore();
        score.setUserLogin(LOGIN);
        score.setUpdatedAt(START);
        when(scoreRepository.findForUpdate(LOGIN)).thenReturn(Optional.of(score));

        question = question(1L, Difficulty.EASY);
        setRules(POINTS, PENALTY, TIME_LIMIT);
        when(gameSettingsService.getSoundUrl(SoundEvent.SUCCESS)).thenReturn(Optional.of(SUCCESS_SOUND));
        when(gameSettingsService.getSoundUrl(SoundEvent.FAILURE)).thenReturn(Optional.of(FAILURE_SOUND));

        when(attemptRepository.findById(any())).thenAnswer(invocation ->
            attempts.stream().filter(a -> a.getId().equals(invocation.getArgument(0))).findFirst()
        );
        when(attemptRepository.findByUserLoginAndStatusOrderByIdAsc(any(), any())).thenAnswer(invocation ->
            attempts
                .stream()
                .filter(a -> a.getUserLogin().equals(invocation.getArgument(0)) && a.getStatus() == invocation.getArgument(1))
                .toList()
        );
        when(attemptRepository.save(any())).thenAnswer(invocation -> {
            QuestionAttempt saved = invocation.getArgument(0);
            saved.setId(100L + attempts.size());
            attempts.add(saved);
            return saved;
        });

        when(questionRepository.count(ArgumentMatchers.<Specification<Question>>any())).thenReturn(1L);
        when(questionRepository.findAll(ArgumentMatchers.<Specification<Question>>any(), any(Pageable.class))).thenReturn(
            new PageImpl<>(List.of(question))
        );

        service = new AttemptServiceImpl(
            attemptRepository,
            questionRepository,
            scoreRepository,
            gameSettingsService,
            Mappers.getMapper(QuestionMapper.class),
            new ApplicationProperties(null, new ApplicationProperties.Attempt(GRACE)),
            clock
        );
    }

    // ── Points ─────────────────────────────────────────────────────

    @Test
    void correctAnswerInTimeWinsThePointsOfTheLevel() {
        QuestionAttempt attempt = runningAttempt(LOGIN);
        clock.advanceSeconds(5);

        AnswerResultDTO result = service.answer(LOGIN, attempt.getId(), CORRECT_CHOICE);

        assertThat(result.outcome()).isEqualTo(AnswerOutcome.CORRECT);
        assertThat(result.pointsDelta()).isEqualTo(POINTS);
        assertThat(result.totalPoints()).isEqualTo(POINTS);
        assertThat(result.correctChoiceId()).isEqualTo(CORRECT_CHOICE);
        assertThat(result.soundUrl()).isEqualTo(SUCCESS_SOUND);
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.CORRECT);
        assertThat(score.getCorrectAnswers()).isEqualTo(1);
    }

    @Test
    void secondCorrectAnswerToTheSameQuestionWinsNothing() {
        score.setTotalPoints(POINTS);
        score.setCorrectAnswers(1);
        when(attemptRepository.existsByUserLoginAndQuestion_IdAndStatus(LOGIN, question.getId(), AttemptStatus.CORRECT)).thenReturn(true);
        QuestionAttempt attempt = runningAttempt(LOGIN);

        AnswerResultDTO result = service.answer(LOGIN, attempt.getId(), CORRECT_CHOICE);

        assertThat(result.outcome()).isEqualTo(AnswerOutcome.CORRECT);
        assertThat(result.pointsDelta()).isZero();
        assertThat(result.totalPoints()).isEqualTo(POINTS);
        assertThat(score.getCorrectAnswers()).isEqualTo(1);
    }

    // ── Penalty ────────────────────────────────────────────────────

    @Test
    void wrongAnswerLosesThePenaltyOfTheLevel() {
        score.setTotalPoints(20);
        QuestionAttempt attempt = runningAttempt(LOGIN);

        AnswerResultDTO result = service.answer(LOGIN, attempt.getId(), WRONG_CHOICE);

        assertThat(result.outcome()).isEqualTo(AnswerOutcome.WRONG);
        assertThat(result.pointsDelta()).isEqualTo(-PENALTY);
        assertThat(result.totalPoints()).isEqualTo(15);
        assertThat(result.correctChoiceId()).isEqualTo(CORRECT_CHOICE);
        assertThat(result.soundUrl()).isEqualTo(FAILURE_SOUND);
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.WRONG);
    }

    @Test
    void penaltyNeverTakesTheScoreBelowZero() {
        score.setTotalPoints(3);
        QuestionAttempt attempt = runningAttempt(LOGIN);

        AnswerResultDTO result = service.answer(LOGIN, attempt.getId(), WRONG_CHOICE);

        // Only the points actually lost are reported
        assertThat(result.pointsDelta()).isEqualTo(-3);
        assertThat(result.totalPoints()).isZero();
    }

    @Test
    void wrongAnswerWithNoPointsLosesNothing() {
        QuestionAttempt attempt = runningAttempt(LOGIN);

        AnswerResultDTO result = service.answer(LOGIN, attempt.getId(), WRONG_CHOICE);

        assertThat(result.pointsDelta()).isZero();
        assertThat(result.totalPoints()).isZero();
    }

    @Test
    void penaltySetToZeroDisablesTheLoss() {
        setRules(POINTS, 0, TIME_LIMIT);
        score.setTotalPoints(20);
        QuestionAttempt attempt = runningAttempt(LOGIN);

        AnswerResultDTO result = service.answer(LOGIN, attempt.getId(), WRONG_CHOICE);

        assertThat(result.outcome()).isEqualTo(AnswerOutcome.WRONG);
        assertThat(result.pointsDelta()).isZero();
        assertThat(result.totalPoints()).isEqualTo(20);
    }

    // ── Timer ──────────────────────────────────────────────────────

    @Test
    void answerAfterTheTimerIsNotCountedAndCostsThePenalty() {
        score.setTotalPoints(20);
        QuestionAttempt attempt = runningAttempt(LOGIN);
        clock.advanceSeconds(TIME_LIMIT + GRACE + 1);

        // The right choice, but too late
        AnswerResultDTO result = service.answer(LOGIN, attempt.getId(), CORRECT_CHOICE);

        assertThat(result.outcome()).isEqualTo(AnswerOutcome.TIME_EXPIRED);
        assertThat(result.pointsDelta()).isEqualTo(-PENALTY);
        assertThat(result.totalPoints()).isEqualTo(15);
        assertThat(result.soundUrl()).isEqualTo(FAILURE_SOUND);
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.EXPIRED);
        assertThat(attempt.getChoice()).isNull();
        assertThat(score.getCorrectAnswers()).isZero();
    }

    @Test
    void answerWithinTheGracePeriodIsAccepted() {
        QuestionAttempt attempt = runningAttempt(LOGIN);
        clock.advanceSeconds(TIME_LIMIT + GRACE);

        AnswerResultDTO result = service.answer(LOGIN, attempt.getId(), CORRECT_CHOICE);

        assertThat(result.outcome()).isEqualTo(AnswerOutcome.CORRECT);
    }

    @Test
    void abandonedAttemptIsSettledWithThePenaltyOnlyOnce() {
        score.setTotalPoints(20);
        QuestionAttempt attempt = runningAttempt(LOGIN);
        clock.advanceSeconds(120);

        service.settleExpired(LOGIN);
        service.settleExpired(LOGIN);

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.EXPIRED);
        assertThat(attempt.getPointsDelta()).isEqualTo(-PENALTY);
        assertThat(score.getTotalPoints()).isEqualTo(15);
    }

    @Test
    void answeringAnAttemptAlreadySettledRepeatsTheOutcomeWithoutASecondPenalty() {
        score.setTotalPoints(20);
        QuestionAttempt attempt = runningAttempt(LOGIN);
        clock.advanceSeconds(120);
        service.settleExpired(LOGIN);

        AnswerResultDTO result = service.answer(LOGIN, attempt.getId(), CORRECT_CHOICE);

        assertThat(result.outcome()).isEqualTo(AnswerOutcome.TIME_EXPIRED);
        assertThat(result.pointsDelta()).isEqualTo(-PENALTY);
        assertThat(result.totalPoints()).isEqualTo(15);
    }

    @Test
    void runningAttemptIsNotSettled() {
        score.setTotalPoints(20);
        QuestionAttempt attempt = runningAttempt(LOGIN);
        clock.advanceSeconds(10);

        service.settleExpired(LOGIN);

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.OPEN);
        assertThat(score.getTotalPoints()).isEqualTo(20);
    }

    // ── Handing out questions ──────────────────────────────────────

    @Test
    void nextStartsATimerWithTheTimeLimitOfTheLevel() {
        StartedQuestionDTO started = service.next(LOGIN, null, null).orElseThrow();

        assertThat(started.question().id()).isEqualTo(question.getId());
        assertThat(started.question().choices()).hasSize(2);
        assertThat(started.timeLimitSeconds()).isEqualTo(TIME_LIMIT);
        assertThat(started.remainingSeconds()).isEqualTo(TIME_LIMIT);
        assertThat(started.startedAt()).isEqualTo(START);
        assertThat(started.expiresAt()).isEqualTo(START.plusSeconds(TIME_LIMIT));
        assertThat(attempts).singleElement().extracting(QuestionAttempt::getStatus).isEqualTo(AttemptStatus.OPEN);
    }

    @Test
    void askingAgainReturnsTheSameAttemptWithoutRestartingTheTimer() {
        StartedQuestionDTO first = service.next(LOGIN, null, null).orElseThrow();
        clock.advanceSeconds(10);

        StartedQuestionDTO second = service.next(LOGIN, null, null).orElseThrow();

        assertThat(second.attemptId()).isEqualTo(first.attemptId());
        assertThat(second.expiresAt()).isEqualTo(first.expiresAt());
        assertThat(second.remainingSeconds()).isEqualTo(TIME_LIMIT - 10);
        verify(attemptRepository, times(1)).save(any());
    }

    @Test
    void changingTheTimeLimitDoesNotMoveARunningTimer() {
        StartedQuestionDTO first = service.next(LOGIN, null, null).orElseThrow();
        setRules(POINTS, PENALTY, 5);
        clock.advanceSeconds(10);

        StartedQuestionDTO second = service.next(LOGIN, null, null).orElseThrow();
        AnswerResultDTO result = service.answer(LOGIN, first.attemptId(), CORRECT_CHOICE);

        assertThat(second.expiresAt()).isEqualTo(START.plusSeconds(TIME_LIMIT));
        assertThat(result.outcome()).isEqualTo(AnswerOutcome.CORRECT);
    }

    @Test
    void afterAnExpiredAttemptTheQuestionCanBeStartedAgainWithANewTimer() {
        score.setTotalPoints(20);
        QuestionAttempt expired = runningAttempt(LOGIN);
        clock.advanceSeconds(120);

        StartedQuestionDTO started = service.next(LOGIN, null, null).orElseThrow();

        assertThat(expired.getStatus()).isEqualTo(AttemptStatus.EXPIRED);
        assertThat(score.getTotalPoints()).isEqualTo(15);
        assertThat(started.attemptId()).isNotEqualTo(expired.getId());
        assertThat(started.question().id()).isEqualTo(question.getId());
        assertThat(started.expiresAt()).isEqualTo(START.plusSeconds(120 + TIME_LIMIT));
    }

    @Test
    void nextIsEmptyWhenNoQuestionIsLeft() {
        when(questionRepository.count(ArgumentMatchers.<Specification<Question>>any())).thenReturn(0L);

        assertThat(service.next(LOGIN, null, null)).isEmpty();
        verify(attemptRepository, never()).save(any());
    }

    // ── Refusals ───────────────────────────────────────────────────

    @Test
    void attemptAlreadyAnsweredIsRefused() {
        QuestionAttempt attempt = runningAttempt(LOGIN);
        service.answer(LOGIN, attempt.getId(), CORRECT_CHOICE);

        assertThatThrownBy(() -> service.answer(LOGIN, attempt.getId(), CORRECT_CHOICE)).isInstanceOf(ConflictException.class);
        assertThat(score.getTotalPoints()).isEqualTo(POINTS);
    }

    @Test
    void attemptOfSomeoneElseIsRefused() {
        QuestionAttempt attempt = runningAttempt("bob");

        assertThatThrownBy(() -> service.answer(LOGIN, attempt.getId(), CORRECT_CHOICE)).isInstanceOf(NotFoundException.class);
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.OPEN);
    }

    @Test
    void choiceOfAnotherQuestionIsRefused() {
        QuestionAttempt attempt = runningAttempt(LOGIN);

        assertThatThrownBy(() -> service.answer(LOGIN, attempt.getId(), 999L)).isInstanceOf(BadRequestException.class);
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.OPEN);
    }

    // ── Fixtures ───────────────────────────────────────────────────

    private void setRules(int points, int penalty, int timeLimit) {
        when(gameSettingsService.getDifficulty(Difficulty.EASY)).thenReturn(
            new DifficultySettingDTO(Difficulty.EASY, points, penalty, timeLimit)
        );
    }

    /** An attempt started at the current time of the clock. */
    private QuestionAttempt runningAttempt(String login) {
        QuestionAttempt attempt = new QuestionAttempt();
        attempt.setId(100L + attempts.size());
        attempt.setUserLogin(login);
        attempt.setQuestion(question);
        attempt.setStatus(AttemptStatus.OPEN);
        attempt.setStartedAt(clock.instant());
        attempt.setExpiresAt(clock.instant().plusSeconds(TIME_LIMIT));
        attempts.add(attempt);
        return attempt;
    }

    private static Question question(Long id, Difficulty difficulty) {
        Category category = new Category();
        category.setId(1L);
        Question question = new Question();
        question.setId(id);
        question.setText("Which word means the opposite of \"hot\"?");
        question.setDifficulty(difficulty);
        question.setCategory(category);
        question.addChoice(choice(CORRECT_CHOICE, "Cold", true));
        question.addChoice(choice(WRONG_CHOICE, "Warm", false));
        return question;
    }

    private static Choice choice(long id, String text, boolean correct) {
        Choice choice = new Choice();
        choice.setId(id);
        choice.setType(ChoiceType.TEXT);
        choice.setText(text);
        choice.setCorrect(correct);
        return choice;
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
