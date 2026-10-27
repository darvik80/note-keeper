package dev.cogitor.note.keeper.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import dev.cogitor.note.keeper.dto.NoteTemplateInput;
import dev.cogitor.note.keeper.exception.AccessDeniedException;
import dev.cogitor.note.keeper.exception.ResourceNotFoundException;
import dev.cogitor.note.keeper.mapper.TemplateMapper;
import dev.cogitor.note.keeper.model.NoteTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateMapper templateMapper;

    private static final String SYSTEM_OWNER = "system";

    public List<NoteTemplate> findAll(String category, String ownerId) {
        return templateMapper.findAll(category, ownerId);
    }

    public NoteTemplate findById(String id, String ownerId) {
        NoteTemplate template = templateMapper.findById(id);
        if (template == null) {
            throw new ResourceNotFoundException("Template not found: " + id);
        }
        // System templates are readable by everyone
        if (SYSTEM_OWNER.equals(template.getOwnerId())) {
            return template;
        }
        requireOwner(template, ownerId);
        return template;
    }

    public NoteTemplate create(NoteTemplateInput input, String ownerId) {
        NoteTemplate template = new NoteTemplate();
        template.setId(UUID.randomUUID().toString());
        template.setOwnerId(ownerId);
        template.setName(input.getName());
        template.setContent(input.getContent());
        template.setTags(input.getTags() != null ? input.getTags() : new ArrayList<>());
        template.setCategory(input.getCategory());
        template.setCreatedAt(LocalDateTime.now());

        templateMapper.insert(template);
        return findById(template.getId(), ownerId);
    }

    public void delete(String id, String ownerId) {
        NoteTemplate template = loadTemplate(id);
        if (SYSTEM_OWNER.equals(template.getOwnerId())) {
            throw new AccessDeniedException("Cannot delete system templates");
        }
        requireOwner(template, ownerId);
        templateMapper.delete(id);
    }

    private NoteTemplate loadTemplate(String id) {
        NoteTemplate template = templateMapper.findById(id);
        if (template == null) {
            throw new ResourceNotFoundException("Template not found: " + id);
        }
        return template;
    }

    private void requireOwner(NoteTemplate template, String ownerId) {
        if (template.getOwnerId() == null || !template.getOwnerId().equals(ownerId)) {
            throw new AccessDeniedException("Not authorized to access this template");
        }
    }
}
