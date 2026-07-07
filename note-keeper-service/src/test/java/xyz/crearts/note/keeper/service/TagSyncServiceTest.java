package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.crearts.note.keeper.mapper.NoteMapper;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.mapper.UserTagMapper;
import xyz.crearts.note.keeper.model.Note;
import xyz.crearts.note.keeper.model.Todo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagSyncServiceTest {

    @Mock private UserTagMapper userTagMapper;
    @Mock private NoteMapper noteMapper;
    @Mock private TodoMapper todoMapper;

    private TagSyncService tagSyncService;

    @BeforeEach
    void setUp() {
        tagSyncService = new TagSyncService(userTagMapper, noteMapper, todoMapper);
    }

    @Test
    void addTags_shouldInsertEachTag() {
        tagSyncService.addTags("owner-1", List.of("work", "urgent"));

        verify(userTagMapper, times(2)).insert(anyString(), eq("owner-1"), anyString());
    }

    @Test
    void addTags_nullList_shouldDoNothing() {
        tagSyncService.addTags("owner-1", null);

        verify(userTagMapper, never()).insert(anyString(), anyString(), anyString());
    }

    @Test
    void addTags_blankTags_shouldSkip() {
        tagSyncService.addTags("owner-1", List.of("", "  ", "valid"));

        verify(userTagMapper, times(1)).insert(anyString(), eq("owner-1"), eq("valid"));
    }

    @Test
    void removeTagsIfUnused_tagNotInUse_shouldDelete() {
        when(noteMapper.findAll(isNull(), eq("old-tag"), isNull(), isNull(), isNull(), isNull(), isNull(), eq("owner-1")))
                .thenReturn(Collections.emptyList());
        when(todoMapper.findAll(isNull(), eq("old-tag"), isNull(), isNull(), isNull(), isNull(), eq("owner-1")))
                .thenReturn(Collections.emptyList());

        tagSyncService.removeTagsIfUnused("owner-1", List.of("old-tag"));

        verify(userTagMapper).deleteByName("owner-1", "old-tag");
    }

    @Test
    void removeTagsIfUnused_tagStillInUse_shouldNotDelete() {
        Note note = new Note();
        note.setTags(List.of("active-tag"));
        when(noteMapper.findAll(isNull(), eq("active-tag"), isNull(), isNull(), isNull(), isNull(), isNull(), eq("owner-1")))
                .thenReturn(List.of(note));

        tagSyncService.removeTagsIfUnused("owner-1", List.of("active-tag"));

        verify(userTagMapper, never()).deleteByName(anyString(), anyString());
    }

    @Test
    void removeTagsIfUnused_nullList_shouldDoNothing() {
        tagSyncService.removeTagsIfUnused("owner-1", null);

        verify(userTagMapper, never()).deleteByName(anyString(), anyString());
    }

    @Test
    void updateTags_shouldAddNewAndRemoveOld() {
        List<String> oldTags = List.of("old1", "shared");
        List<String> newTags = List.of("new1", "shared");

        // "old1" is not used by any other note/todo
        when(noteMapper.findAll(isNull(), eq("old1"), isNull(), isNull(), isNull(), isNull(), isNull(), eq("owner-1")))
                .thenReturn(Collections.emptyList());
        when(todoMapper.findAll(isNull(), eq("old1"), isNull(), isNull(), isNull(), isNull(), eq("owner-1")))
                .thenReturn(Collections.emptyList());

        tagSyncService.updateTags("owner-1", oldTags, newTags);

        // "new1" should be added
        verify(userTagMapper).insert(anyString(), eq("owner-1"), eq("new1"));
        // "old1" should be removed (not in use)
        verify(userTagMapper).deleteByName("owner-1", "old1");
        // "shared" should NOT be added or removed
        verify(userTagMapper, never()).insert(anyString(), eq("owner-1"), eq("shared"));
    }

    @Test
    void getAllTags_shouldCallMapper() {
        when(userTagMapper.findAllByOwner("owner-1")).thenReturn(List.of("tag1", "tag2"));

        List<String> tags = tagSyncService.getAllTags("owner-1");

        assertEquals(2, tags.size());
        verify(userTagMapper).findAllByOwner("owner-1");
    }

    @Test
    void rebuildForOwner_shouldClearAndRepopulate() {
        when(userTagMapper.findAllByOwner("owner-1")).thenReturn(List.of("old-tag"));

        Note note = new Note();
        note.setTags(List.of("from-note"));
        when(noteMapper.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq("owner-1")))
                .thenReturn(List.of(note));

        Todo todo = new Todo();
        todo.setTags(List.of("from-todo"));
        when(todoMapper.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq("owner-1")))
                .thenReturn(List.of(todo));

        tagSyncService.rebuildForOwner("owner-1");

        // Old tags deleted
        verify(userTagMapper).deleteByName("owner-1", "old-tag");
        // New tags added
        verify(userTagMapper, atLeastOnce()).insert(anyString(), eq("owner-1"), anyString());
    }
}
