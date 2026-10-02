package com.lingua.learning.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingua.learning.domain.Category;
import com.lingua.learning.domain.Choice;
import com.lingua.learning.domain.Question;
import com.lingua.learning.domain.enumeration.ChoiceType;
import com.lingua.learning.domain.enumeration.Difficulty;
import com.lingua.learning.service.dto.ChoiceAdminDTO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class QuestionMapperTest {

    private final QuestionMapper mapper = Mappers.getMapper(QuestionMapper.class);

    @Test
    void learnerViewNeverRevealsTheCorrectChoice() throws Exception {
        String json = new ObjectMapper().writeValueAsString(mapper.toDto(question()));

        assertThat(json).contains("Cold", "Warm").doesNotContain("correct");
    }

    @Test
    void adminViewShowsTheCorrectChoice() {
        assertThat(mapper.toAdminDto(question()).choices()).extracting(ChoiceAdminDTO::correct).containsExactly(true, false);
    }

    private static Question question() {
        Category category = new Category();
        category.setId(1L);
        Question question = new Question();
        question.setId(1L);
        question.setText("Which word means the opposite of \"hot\"?");
        question.setDifficulty(Difficulty.EASY);
        question.setCategory(category);
        question.addChoice(choice(10L, "Cold", true));
        question.addChoice(choice(11L, "Warm", false));
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
}
