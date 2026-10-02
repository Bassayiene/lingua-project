package com.lingua.learning.domain;

import com.lingua.learning.domain.enumeration.Difficulty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Rules of one difficulty level, adjustable by the admin. There is exactly one row per level.
 */
@Entity
@Table(name = "difficulty_setting")
public class DifficultySetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, unique = true, length = 20)
    private Difficulty difficulty;

    /** Points won by the first correct answer to a question of this level. */
    @Column(name = "points", nullable = false)
    private int points;

    /** Points lost by a wrong answer or an expired timer. */
    @Column(name = "penalty_points", nullable = false)
    private int penaltyPoints;

    /** Time allowed to answer a question of this level. */
    @Column(name = "time_limit_seconds", nullable = false)
    private int timeLimitSeconds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getPenaltyPoints() {
        return penaltyPoints;
    }

    public void setPenaltyPoints(int penaltyPoints) {
        this.penaltyPoints = penaltyPoints;
    }

    public int getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public void setTimeLimitSeconds(int timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }
}
