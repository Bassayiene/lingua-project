package com.lingua.learning.repository;

import com.lingua.learning.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @EntityGraph(attributePaths = "language")
    List<Category> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "language")
    List<Category> findByLanguage_CodeOrderByNameAsc(String languageCode);
}
