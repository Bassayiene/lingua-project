package com.lingua.learning.service.mapper;

import com.lingua.learning.domain.Choice;
import com.lingua.learning.domain.Question;
import com.lingua.learning.service.dto.ChoiceAdminDTO;
import com.lingua.learning.service.dto.ChoiceDTO;
import com.lingua.learning.service.dto.QuestionAdminDTO;
import com.lingua.learning.service.dto.QuestionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Two views of a question: the learner view hides which choice is correct, the admin view shows it.
 */
@Mapper(componentModel = "spring")
public interface QuestionMapper {
    @Mapping(target = "categoryId", source = "category.id")
    QuestionDTO toDto(Question question);

    ChoiceDTO toDto(Choice choice);

    @Mapping(target = "categoryId", source = "category.id")
    QuestionAdminDTO toAdminDto(Question question);

    ChoiceAdminDTO toAdminDto(Choice choice);
}
