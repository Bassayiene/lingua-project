package com.lingua.learning.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lingua.learning.domain.Category;
import com.lingua.learning.domain.Choice;
import com.lingua.learning.domain.Question;
import com.lingua.learning.domain.enumeration.ChoiceType;
import com.lingua.learning.domain.enumeration.Difficulty;
import com.lingua.learning.exception.BadRequestException;
import com.lingua.learning.repository.CategoryRepository;
import com.lingua.learning.repository.QuestionRepository;
import com.lingua.learning.service.dto.ChoiceAdminDTO;
import com.lingua.learning.service.dto.QuestionAdminDTO;
import com.lingua.learning.service.mapper.QuestionMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuestionServiceImplTest {

    private static final Long CATEGORY_ID = 1L;
    private static final String IMAGE_URL = "http://localhost:9000/learning-images/cat.jpg";

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private QuestionServiceImpl service;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setId(CATEGORY_ID);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(questionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service = new QuestionServiceImpl(
            questionRepository,
            categoryRepository,
            Mappers.getMapper(QuestionMapper.class),
            Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void questionWithATextChoiceAndAnImageChoiceIsCreated() {
        // Each choice carries both contents: only the one matching its type must be kept
        QuestionAdminDTO created = service.create(
            question(
                new ChoiceAdminDTO(null, ChoiceType.TEXT, " A cat ", "http://ignored", false),
                new ChoiceAdminDTO(null, ChoiceType.IMAGE, "ignored", IMAGE_URL, true)
            )
        );

        ArgumentCaptor<Question> saved = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).saveAndFlush(saved.capture());
        List<Choice> choices = saved.getValue().getChoices();
        assertThat(choices).extracting(Choice::getText).containsExactly("A cat", null);
        assertThat(choices).extracting(Choice::getImageUrl).containsExactly(null, IMAGE_URL);
        assertThat(choices).extracting(Choice::isCorrect).containsExactly(false, true);
        assertThat(choices).extracting(Choice::getPosition).containsExactly(0, 1);
        assertThat(choices).allSatisfy(choice -> assertThat(choice.getQuestion()).isSameAs(saved.getValue()));

        assertThat(created.difficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(created.categoryId()).isEqualTo(CATEGORY_ID);
        assertThat(created.createdAt()).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Test
    void questionWithFewerThanTwoChoicesIsRefused() {
        assertRefused(question(text("Cold", true)));
        assertRefused(question());
    }

    @Test
    void questionWithoutACorrectChoiceIsRefused() {
        assertRefused(question(text("Cold", false), text("Warm", false)));
    }

    @Test
    void questionWithSeveralCorrectChoicesIsRefused() {
        assertRefused(question(text("Cold", true), text("Warm", true), text("Big", false)));
    }

    @Test
    void textChoiceWithoutTextIsRefused() {
        assertRefused(question(text("Cold", true), text("  ", false)));
    }

    @Test
    void imageChoiceWithoutAnHttpUrlIsRefused() {
        assertRefused(question(text("Cold", true), new ChoiceAdminDTO(null, ChoiceType.IMAGE, "A picture", null, false)));
        assertRefused(question(text("Cold", true), new ChoiceAdminDTO(null, ChoiceType.IMAGE, null, "javascript:alert(1)", false)));
    }

    @Test
    void unknownCategoryIsRefused() {
        QuestionAdminDTO dto = new QuestionAdminDTO(
            null,
            "Question?",
            Difficulty.EASY,
            999L,
            null,
            List.of(text("Cold", true), text("Warm", false))
        );

        assertRefused(dto);
    }

    @Test
    void updateReplacesTheChoices() {
        Question existing = new Question();
        existing.setId(5L);
        Choice old = new Choice();
        old.setId(50L);
        existing.addChoice(old);
        when(questionRepository.findById(5L)).thenReturn(Optional.of(existing));

        service.update(5L, question(text("Cold", true), text("Warm", false), text("Big", false)));

        assertThat(existing.getChoices()).hasSize(3).doesNotContain(old);
        assertThat(existing.getChoices()).extracting(Choice::getText).containsExactly("Cold", "Warm", "Big");
    }

    private void assertRefused(QuestionAdminDTO dto) {
        assertThatThrownBy(() -> service.create(dto)).isInstanceOf(BadRequestException.class);
        verify(questionRepository, never()).saveAndFlush(any());
    }

    private static QuestionAdminDTO question(ChoiceAdminDTO... choices) {
        return new QuestionAdminDTO(null, "Which one is a cat?", Difficulty.MEDIUM, CATEGORY_ID, null, List.of(choices));
    }

    private static ChoiceAdminDTO text(String text, boolean correct) {
        return new ChoiceAdminDTO(null, ChoiceType.TEXT, text, null, correct);
    }
}
