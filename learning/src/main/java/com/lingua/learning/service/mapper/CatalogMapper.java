package com.lingua.learning.service.mapper;

import com.lingua.learning.domain.Category;
import com.lingua.learning.domain.Language;
import com.lingua.learning.service.dto.CategoryDTO;
import com.lingua.learning.service.dto.LanguageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CatalogMapper {
    LanguageDTO toDto(Language language);

    @Mapping(target = "languageId", source = "language.id")
    @Mapping(target = "languageCode", source = "language.code")
    CategoryDTO toDto(Category category);
}
