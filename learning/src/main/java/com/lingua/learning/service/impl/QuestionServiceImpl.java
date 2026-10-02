package com.lingua.learning.service.impl;

import static com.lingua.learning.repository.QuestionSpecifications.inCategory;
import static com.lingua.learning.repository.QuestionSpecifications.withDifficulty;

import com.lingua.learning.domain.Category;
import com.lingua.learning.domain.Choice;
import com.lingua.learning.domain.Question;
import com.lingua.learning.domain.enumeration.ChoiceType;
import com.lingua.learning.domain.enumeration.Difficulty;
import com.lingua.learning.exception.BadRequestException;
import com.lingua.learning.exception.NotFoundException;
import com.lingua.learning.repository.CategoryRepository;
import com.lingua.learning.repository.QuestionRepository;
import com.lingua.learning.service.QuestionService;
import com.lingua.learning.service.dto.ChoiceAdminDTO;
import com.lingua.learning.service.dto.QuestionAdminDTO;
import com.lingua.learning.service.mapper.QuestionMapper;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Service Implementation for managing questions and their choices.
 */
@Service
@Transactional
public class QuestionServiceImpl implements QuestionService {

    public static final int MIN_CHOICES = 2;

    private final QuestionRepository questionRepository;

    private final CategoryRepository categoryRepository;

    private final QuestionMapper questionMapper;

    private final Clock clock;

    public QuestionServiceImpl(
        QuestionRepository questionRepository,
        CategoryRepository categoryRepository,
        QuestionMapper questionMapper,
        Clock clock
    ) {
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
        this.questionMapper = questionMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionAdminDTO> findAll(Long categoryId, Difficulty difficulty, Pageable pageable) {
        return questionRepository
            .findAll(Specification.allOf(inCategory(categoryId), withDifficulty(difficulty)), pageable)
            .map(questionMapper::toAdminDto);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionAdminDTO findOne(Long id) {
        return questionMapper.toAdminDto(find(id));
    }

    @Override
    public QuestionAdminDTO create(QuestionAdminDTO dto) {
        validateChoices(dto.choices());
        Question question = new Question();
        question.setCreatedAt(clock.instant());
        apply(question, dto);
        return questionMapper.toAdminDto(questionRepository.saveAndFlush(question));
    }

    @Override
    public QuestionAdminDTO update(Long id, QuestionAdminDTO dto) {
        validateChoices(dto.choices());
        Question question = find(id);
        apply(question, dto);
        return questionMapper.toAdminDto(questionRepository.saveAndFlush(question));
    }

    @Override
    public void delete(Long id) {
        questionRepository.delete(find(id));
    }

    /**
     * A question needs at least two choices, exactly one correct, and each choice must carry
     * the content of its type.
     */
    private void validateChoices(List<ChoiceAdminDTO> choices) {
        if (choices == null || choices.size() < MIN_CHOICES) {
            throw new BadRequestException("Une question doit avoir au moins " + MIN_CHOICES + " choix");
        }
        long correct = choices.stream().filter(ChoiceAdminDTO::correct).count();
        if (correct != 1) {
            throw new BadRequestException("Une question doit avoir exactement un choix correct (reçu : " + correct + ")");
        }
        for (ChoiceAdminDTO choice : choices) {
            if (choice.type() == null) {
                throw new BadRequestException("Chaque choix doit avoir un type (TEXT ou IMAGE)");
            }
            if (choice.type() == ChoiceType.TEXT && !StringUtils.hasText(choice.text())) {
                throw new BadRequestException("Un choix de type TEXT doit avoir un texte");
            }
            if (choice.type() == ChoiceType.IMAGE && !isHttpUrl(choice.imageUrl())) {
                throw new BadRequestException("Un choix de type IMAGE doit avoir une URL d'image http(s)");
            }
        }
    }

    private static boolean isHttpUrl(String value) {
        return value != null && (value.startsWith("http://") || value.startsWith("https://"));
    }

    private void apply(Question question, QuestionAdminDTO dto) {
        Category category = categoryRepository
            .findById(dto.categoryId())
            .orElseThrow(() -> new BadRequestException("Catégorie inconnue : " + dto.categoryId()));
        question.setText(dto.text().trim());
        question.setDifficulty(dto.difficulty());
        question.setCategory(category);

        question.getChoices().clear();
        int position = 0;
        for (ChoiceAdminDTO dtoChoice : dto.choices()) {
            Choice choice = new Choice();
            choice.setType(dtoChoice.type());
            // Only the content matching the type is kept
            choice.setText(dtoChoice.type() == ChoiceType.TEXT ? dtoChoice.text().trim() : null);
            choice.setImageUrl(dtoChoice.type() == ChoiceType.IMAGE ? dtoChoice.imageUrl() : null);
            choice.setCorrect(dtoChoice.correct());
            choice.setPosition(position++);
            question.addChoice(choice);
        }
    }

    private Question find(Long id) {
        return questionRepository.findById(id).orElseThrow(() -> new NotFoundException("Question introuvable : " + id));
    }
}
