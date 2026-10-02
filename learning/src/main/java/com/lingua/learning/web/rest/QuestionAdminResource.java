package com.lingua.learning.web.rest;

import com.lingua.learning.domain.enumeration.Difficulty;
import com.lingua.learning.service.QuestionService;
import com.lingua.learning.service.dto.QuestionAdminDTO;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link com.lingua.learning.domain.Question} and their choices.
 * Reserved to administrators: these endpoints expose which choice is correct.
 */
@RestController
@RequestMapping("/api/admin/questions")
public class QuestionAdminResource {

    private final QuestionService questionService;

    public QuestionAdminResource(QuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * {@code GET /admin/questions} : get a page of questions, optionally filtered.
     * The total number of questions is returned in the {@code X-Total-Count} header.
     */
    @GetMapping("")
    public ResponseEntity<List<QuestionAdminDTO>> getQuestions(
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Difficulty difficulty,
        @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<QuestionAdminDTO> page = questionService.findAll(categoryId, difficulty, pageable);
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(page.getTotalElements())).body(page.getContent());
    }

    /**
     * {@code GET /admin/questions/:id} : get a question with its choices.
     */
    @GetMapping("/{id}")
    public QuestionAdminDTO getQuestion(@PathVariable Long id) {
        return questionService.findOne(id);
    }

    /**
     * {@code POST /admin/questions} : create a question with its choices.
     *
     * @return status {@code 201 (Created)}, or {@code 400 (Bad Request)} if the question has fewer than
     *         two choices, not exactly one correct choice, or a choice without the content of its type.
     */
    @PostMapping("")
    public ResponseEntity<QuestionAdminDTO> createQuestion(@Valid @RequestBody QuestionAdminDTO question) {
        QuestionAdminDTO created = questionService.create(question);
        return ResponseEntity.created(URI.create("/api/admin/questions/" + created.id())).body(created);
    }

    /**
     * {@code PUT /admin/questions/:id} : replace a question and all its choices.
     */
    @PutMapping("/{id}")
    public QuestionAdminDTO updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionAdminDTO question) {
        return questionService.update(id, question);
    }

    /**
     * {@code DELETE /admin/questions/:id} : delete a question, its choices and its attempts.
     * Points already won or lost on it are kept.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
