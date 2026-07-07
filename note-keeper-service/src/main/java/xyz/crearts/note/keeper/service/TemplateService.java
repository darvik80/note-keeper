package xyz.crearts.note.keeper.service;

import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.dto.NoteTemplateInput;
import xyz.crearts.note.keeper.exception.AccessDeniedException;
import xyz.crearts.note.keeper.exception.ResourceNotFoundException;
import xyz.crearts.note.keeper.mapper.TemplateMapper;
import xyz.crearts.note.keeper.model.NoteTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TemplateService {

    private final TemplateMapper templateMapper;

    public TemplateService(TemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    public List<NoteTemplate> findAll(String category, String ownerId) {
        return templateMapper.findAll(category, ownerId);
    }

    public NoteTemplate findById(String id, String ownerId) {
        NoteTemplate template = templateMapper.findById(id);
        if (template == null) {
            throw new ResourceNotFoundException("Template not found: " + id);
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
        requireOwner(loadTemplate(id), ownerId);
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
