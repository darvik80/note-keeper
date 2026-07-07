package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.crearts.note.keeper.dto.NoteTemplateInput;
import xyz.crearts.note.keeper.exception.AccessDeniedException;
import xyz.crearts.note.keeper.exception.ResourceNotFoundException;
import xyz.crearts.note.keeper.mapper.TemplateMapper;
import xyz.crearts.note.keeper.model.NoteTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock private TemplateMapper templateMapper;

    private TemplateService templateService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        templateService = new TemplateService(templateMapper);
    }

    @Test
    void findAll_shouldReturnTemplates() {
        when(templateMapper.findAll(null, "owner-1")).thenReturn(Collections.emptyList());

        List<NoteTemplate> result = templateService.findAll(null, "owner-1");

        assertNotNull(result);
        verify(templateMapper).findAll(null, "owner-1");
    }

    @Test
    void findById_existingOwner_shouldReturnTemplate() {
        NoteTemplate template = new NoteTemplate();
        template.setId("tpl-1");
        template.setOwnerId("owner-1");
        when(templateMapper.findById("tpl-1")).thenReturn(template);

        NoteTemplate result = templateService.findById("tpl-1", "owner-1");

        assertEquals("tpl-1", result.getId());
    }

    @Test
    void findById_otherOwner_shouldThrowAccessDenied() {
        NoteTemplate template = new NoteTemplate();
        template.setId("tpl-1");
        template.setOwnerId("owner-1");
        when(templateMapper.findById("tpl-1")).thenReturn(template);

        assertThrows(AccessDeniedException.class, () -> templateService.findById("tpl-1", "other-user"));
    }

    @Test
    void create_shouldInsertWithOwner() {
        NoteTemplateInput input = new NoteTemplateInput();
        input.setName("New Template");
        input.setContent("Template content");
        input.setCategory("general");

        NoteTemplate created = new NoteTemplate();
        created.setId("new-id");
        created.setOwnerId("owner-1");
        when(templateMapper.findById(anyString())).thenReturn(created);

        templateService.create(input, "owner-1");

        verify(templateMapper).insert(argThat(t -> "owner-1".equals(t.getOwnerId())));
    }

    @Test
    void delete_nonExistent_shouldThrowException() {
        when(templateMapper.findById("missing")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> templateService.delete("missing", "owner-1"));
    }
}
