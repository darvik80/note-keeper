package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.crearts.note.keeper.dto.NoteTemplateInput;
import xyz.crearts.note.keeper.exception.ResourceNotFoundException;
import xyz.crearts.note.keeper.mapper.TemplateMapper;
import xyz.crearts.note.keeper.model.NoteTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock private TemplateMapper templateMapper;

    private TemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(templateMapper);
    }

    @Test
    void findAll_shouldReturnTemplates() {
        when(templateMapper.findAll(null)).thenReturn(Collections.emptyList());

        List<NoteTemplate> result = templateService.findAll(null);

        assertNotNull(result);
        verify(templateMapper).findAll(null);
    }

    @Test
    void findAll_withCategory_shouldFilterByCategory() {
        when(templateMapper.findAll("meeting")).thenReturn(Collections.emptyList());

        List<NoteTemplate> result = templateService.findAll("meeting");

        verify(templateMapper).findAll("meeting");
    }

    @Test
    void findById_existing_shouldReturnTemplate() {
        NoteTemplate template = new NoteTemplate();
        template.setId("tpl-1");
        template.setName("Meeting Notes");
        when(templateMapper.findById("tpl-1")).thenReturn(template);

        NoteTemplate result = templateService.findById("tpl-1");

        assertEquals("tpl-1", result.getId());
        assertEquals("Meeting Notes", result.getName());
    }

    @Test
    void findById_nonExistent_shouldThrowException() {
        when(templateMapper.findById("missing")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> templateService.findById("missing"));
    }

    @Test
    void create_shouldInsertAndReturnTemplate() {
        NoteTemplateInput input = new NoteTemplateInput();
        input.setName("New Template");
        input.setContent("Template content");
        input.setTags(List.of("work"));
        input.setCategory("general");

        NoteTemplate created = new NoteTemplate();
        created.setId("new-id");
        created.setName("New Template");
        when(templateMapper.findById(anyString())).thenReturn(created);

        NoteTemplate result = templateService.create(input);

        assertNotNull(result);
        verify(templateMapper).insert(any(NoteTemplate.class));
    }

    @Test
    void create_nullTags_shouldDefaultToEmptyList() {
        NoteTemplateInput input = new NoteTemplateInput();
        input.setName("Template");
        input.setTags(null);

        NoteTemplate created = new NoteTemplate();
        created.setId("id-1");
        when(templateMapper.findById(anyString())).thenReturn(created);

        templateService.create(input);

        verify(templateMapper).insert(argThat(t -> t.getTags() != null && t.getTags().isEmpty()));
    }

    @Test
    void delete_existing_shouldCallMapperDelete() {
        NoteTemplate template = new NoteTemplate();
        template.setId("tpl-1");
        when(templateMapper.findById("tpl-1")).thenReturn(template);

        templateService.delete("tpl-1");

        verify(templateMapper).delete("tpl-1");
    }

    @Test
    void delete_nonExistent_shouldThrowException() {
        when(templateMapper.findById("missing")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> templateService.delete("missing"));
    }
}
