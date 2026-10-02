package com.lingua.learning.web.rest;

import com.lingua.learning.service.CatalogService;
import com.lingua.learning.service.dto.LanguageDTO;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link com.lingua.learning.domain.Language}.
 * Reading is public; the {@code /admin} endpoints are reserved to administrators.
 */
@RestController
@RequestMapping("/api")
public class LanguageResource {

    private final CatalogService catalogService;

    public LanguageResource(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * {@code GET /languages} : get all the languages.
     */
    @GetMapping("/languages")
    public List<LanguageDTO> getLanguages() {
        return catalogService.getLanguages();
    }

    /**
     * {@code POST /admin/languages} : create a language.
     *
     * @return status {@code 201 (Created)}, or {@code 409 (Conflict)} if the code already exists.
     */
    @PostMapping("/admin/languages")
    public ResponseEntity<LanguageDTO> createLanguage(@Valid @RequestBody LanguageDTO language) {
        LanguageDTO created = catalogService.createLanguage(language);
        return ResponseEntity.created(URI.create("/api/languages/" + created.id())).body(created);
    }

    /**
     * {@code PUT /admin/languages/:id} : update a language.
     */
    @PutMapping("/admin/languages/{id}")
    public LanguageDTO updateLanguage(@PathVariable Long id, @Valid @RequestBody LanguageDTO language) {
        return catalogService.updateLanguage(id, language);
    }

    /**
     * {@code DELETE /admin/languages/:id} : delete a language.
     *
     * @return status {@code 204 (No Content)}, or {@code 409 (Conflict)} if it still has categories.
     */
    @DeleteMapping("/admin/languages/{id}")
    public ResponseEntity<Void> deleteLanguage(@PathVariable Long id) {
        catalogService.deleteLanguage(id);
        return ResponseEntity.noContent().build();
    }
}
