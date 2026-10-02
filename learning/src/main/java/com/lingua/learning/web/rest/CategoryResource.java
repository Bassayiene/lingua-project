package com.lingua.learning.web.rest;

import com.lingua.learning.service.CatalogService;
import com.lingua.learning.service.dto.CategoryDTO;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
 * REST controller for managing {@link com.lingua.learning.domain.Category}.
 * Reading is public; the {@code /admin} endpoints are reserved to administrators.
 */
@RestController
@RequestMapping("/api")
public class CategoryResource {

    private final CatalogService catalogService;

    public CategoryResource(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * {@code GET /categories} : get the categories.
     *
     * @param languageCode only the categories of this language (for example "en"); all of them if absent.
     */
    @GetMapping("/categories")
    public List<CategoryDTO> getCategories(@RequestParam(required = false) String languageCode) {
        return catalogService.getCategories(languageCode);
    }

    /**
     * {@code POST /admin/categories} : create a category.
     *
     * @return status {@code 201 (Created)}, or {@code 409 (Conflict)} if the language already has a category with this name.
     */
    @PostMapping("/admin/categories")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO category) {
        CategoryDTO created = catalogService.createCategory(category);
        return ResponseEntity.created(URI.create("/api/categories/" + created.id())).body(created);
    }

    /**
     * {@code PUT /admin/categories/:id} : update a category.
     */
    @PutMapping("/admin/categories/{id}")
    public CategoryDTO updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryDTO category) {
        return catalogService.updateCategory(id, category);
    }

    /**
     * {@code DELETE /admin/categories/:id} : delete a category.
     *
     * @return status {@code 204 (No Content)}, or {@code 409 (Conflict)} if it still has questions.
     */
    @DeleteMapping("/admin/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
