package com.lingua.learning.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DifficultySettingUpdateTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void timeLimitMustStayWithinBounds() {
        assertThat(isValid(new DifficultySettingUpdate(10, 5, 4))).isFalse();
        assertThat(isValid(new DifficultySettingUpdate(10, 5, 5))).isTrue();
        assertThat(isValid(new DifficultySettingUpdate(10, 5, 600))).isTrue();
        assertThat(isValid(new DifficultySettingUpdate(10, 5, 601))).isFalse();
    }

    @Test
    void pointsAndPenaltyCannotBeNegative() {
        assertThat(isValid(new DifficultySettingUpdate(-1, 5, 30))).isFalse();
        assertThat(isValid(new DifficultySettingUpdate(10, -1, 30))).isFalse();
        // A penalty of zero is how the admin disables the loss of points
        assertThat(isValid(new DifficultySettingUpdate(0, 0, 30))).isTrue();
    }

    @Test
    void everyValueIsRequired() {
        assertThat(isValid(new DifficultySettingUpdate(null, 5, 30))).isFalse();
        assertThat(isValid(new DifficultySettingUpdate(10, null, 30))).isFalse();
        assertThat(isValid(new DifficultySettingUpdate(10, 5, null))).isFalse();
    }

    private static boolean isValid(DifficultySettingUpdate update) {
        return validator.validate(update).isEmpty();
    }
}
