package xyz.crearts.note.keeper.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import xyz.crearts.note.keeper.dto.NoteTemplateInput;
import xyz.crearts.note.keeper.model.NoteTemplate;
import xyz.crearts.note.keeper.service.TemplateService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public List<NoteTemplate> getTemplates(@RequestParam(required = false) String category,
                                             @AuthenticationPrincipal String ownerId) {
        return templateService.findAll(category, ownerId);
    }

    @PostMapping
    public ResponseEntity<NoteTemplate> createTemplate(@Valid @RequestBody NoteTemplateInput input,
                                                         @AuthenticationPrincipal String ownerId) {
        NoteTemplate template = templateService.create(input, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(template);
    }

    @GetMapping("/{id}")
    public NoteTemplate getTemplateById(@PathVariable String id,
                                        @AuthenticationPrincipal String ownerId) {
        return templateService.findById(id, ownerId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String id,
                                               @AuthenticationPrincipal String ownerId) {
        templateService.delete(id, ownerId);
        return ResponseEntity.noContent().build();
    }
}
