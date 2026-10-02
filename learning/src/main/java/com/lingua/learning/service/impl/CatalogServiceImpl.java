package com.lingua.learning.service.impl;

import com.lingua.learning.domain.Category;
import com.lingua.learning.domain.Language;
import com.lingua.learning.exception.BadRequestException;
import com.lingua.learning.exception.NotFoundException;
import com.lingua.learning.repository.CategoryRepository;
import com.lingua.learning.repository.LanguageRepository;
import com.lingua.learning.service.CatalogService;
import com.lingua.learning.service.dto.CategoryDTO;
import com.lingua.learning.service.dto.LanguageDTO;
import com.lingua.learning.service.mapper.CatalogMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Service Implementation for managing languages and their categories.
 */
@Service
@Transactional
public class CatalogServiceImpl implements CatalogService {

    private final LanguageRepository languageRepository;

    private final CategoryRepository categoryRepository;

    private final CatalogMapper catalogMapper;

    public CatalogServiceImpl(LanguageRepository languageRepository, CategoryRepository categoryRepository, CatalogMapper catalogMapper) {
        this.languageRepository = languageRepository;
        this.categoryRepository = categoryRepository;
        this.catalogMapper = catalogMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LanguageDTO> getLanguages() {
        return languageRepository.findAll(Sort.by("name")).stream().map(catalogMapper::toDto).toList();
    }

    @Override
    public LanguageDTO createLanguage(LanguageDTO dto) {
        Language language = new Language();
        apply(language, dto);
        return catalogMapper.toDto(languageRepository.saveAndFlush(language));
    }

    @Override
    public LanguageDTO updateLanguage(Long id, LanguageDTO dto) {
        Language language = findLanguage(id);
        apply(language, dto);
        return catalogMapper.toDto(languageRepository.saveAndFlush(language));
    }

    @Override
    public void deleteLanguage(Long id) {
        languageRepository.delete(findLanguage(id));
        languageRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategories(String languageCode) {
        List<Category> categories = StringUtils.hasText(languageCode)
            ? categoryRepository.findByLanguage_CodeOrderByNameAsc(languageCode.toLowerCase(Locale.ROOT))
            : categoryRepository.findAllByOrderByNameAsc();
        return categories.stream().map(catalogMapper::toDto).toList();
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO dto) {
        Category category = new Category();
        apply(category, dto);
        return catalogMapper.toDto(categoryRepository.saveAndFlush(category));
    }

    @Override
    public CategoryDTO updateCategory(Long id, CategoryDTO dto) {
        Category category = findCategory(id);
        apply(category, dto);
        return catalogMapper.toDto(categoryRepository.saveAndFlush(category));
    }

    @Override
    public void deleteCategory(Long id) {
        categoryRepository.delete(findCategory(id));
        categoryRepository.flush();
    }

    private void apply(Language language, LanguageDTO dto) {
        language.setCode(dto.code().trim().toLowerCase(Locale.ROOT));
        language.setName(dto.name().trim());
    }

    private void apply(Category category, CategoryDTO dto) {
        category.setName(dto.name().trim());
        category.setDescription(dto.description());
        category.setLanguage(
            languageRepository
                .findById(dto.languageId())
                .orElseThrow(() -> new BadRequestException("Langue inconnue : " + dto.languageId()))
        );
    }

    private Language findLanguage(Long id) {
        return languageRepository.findById(id).orElseThrow(() -> new NotFoundException("Langue introuvable : " + id));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Catégorie introuvable : " + id));
    }
}
