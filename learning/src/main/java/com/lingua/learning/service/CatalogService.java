package com.lingua.learning.service;

import com.lingua.learning.service.dto.CategoryDTO;
import com.lingua.learning.service.dto.LanguageDTO;
import java.util.List;

/**
 * Service Interface for managing languages and their categories.
 */
public interface CatalogService {
    List<LanguageDTO> getLanguages();

    LanguageDTO createLanguage(LanguageDTO language);

    LanguageDTO updateLanguage(Long id, LanguageDTO language);

    void deleteLanguage(Long id);

    /**
     * @param languageCode only the categories of this language, or all of them when null.
     */
    List<CategoryDTO> getCategories(String languageCode);

    CategoryDTO createCategory(CategoryDTO category);

    CategoryDTO updateCategory(Long id, CategoryDTO category);

    void deleteCategory(Long id);
}
