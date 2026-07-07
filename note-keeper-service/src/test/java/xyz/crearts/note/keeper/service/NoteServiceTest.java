package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.crearts.note.keeper.dto.NoteInput;
import xyz.crearts.note.keeper.exception.AccessDeniedException;
import xyz.crearts.note.keeper.exception.ResourceNotFoundException;
import xyz.crearts.note.keeper.mapper.AttachmentMapper;
import xyz.crearts.note.keeper.mapper.NoteHistoryMapper;
import xyz.crearts.note.keeper.mapper.NoteMapper;
import xyz.crearts.note.keeper.model.Note;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock private NoteMapper noteMapper;
    @Mock private NoteHistoryMapper historyMapper;
    @Mock private AttachmentMapper attachmentMapper;
    @Mock private EncryptionService encryptionService;
    @Mock private NotificationService notificationService;
    @Mock private TagSyncService tagSyncService;

    private NoteService noteService;
    private final ResourceAccessService resourceAccess = new ResourceAccessService();

    @BeforeEach
    void setUp() {
        noteService = new NoteService(noteMapper, historyMapper, attachmentMapper,
                encryptionService, notificationService, tagSyncService, resourceAccess);
    }

    private Note buildNote(String id, String ownerId) {
        Note note = new Note();
        note.setId(id);
        note.setTitle("Test Note");
        note.setContent("Test content");
        note.setTags(new ArrayList<>());
        note.setFolder("default");
        note.setPriority("medium");
        note.setOwnerId(ownerId);
        note.setSharedWith("[]");
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        return note;
    }

    @Test
    void findById_existingNote_shouldReturnNote() {
        Note note = buildNote("note-1", "owner-1");
        when(noteMapper.findById("note-1")).thenReturn(note);

        Note result = noteService.findById("note-1", "owner-1");

        assertNotNull(result);
        assertEquals("note-1", result.getId());
    }

    @Test
    void findById_nonExistentNote_shouldThrowException() {
        when(noteMapper.findById("missing")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> noteService.findById("missing", "owner-1"));
    }

    @Test
    void findById_encryptedNote_shouldDecryptContent() {
        Note note = buildNote("note-1", "owner-1");
        note.setEncrypted(true);
        note.setContent("encrypted-content");
        when(noteMapper.findById("note-1")).thenReturn(note);
        when(encryptionService.decrypt("encrypted-content")).thenReturn("decrypted-content");

        Note result = noteService.findById("note-1", "owner-1");

        assertEquals("decrypted-content", result.getContent());
    }

    @Test
    void findAll_shouldReturnDecryptedNotes() {
        Note note = buildNote("note-1", "owner-1");
        note.setEncrypted(true);
        note.setContent("encrypted");
        when(noteMapper.findAll(any(), any(), any(), any(), any(), any(), any(), eq("owner-1")))
                .thenReturn(List.of(note));
        when(encryptionService.decrypt("encrypted")).thenReturn("decrypted");

        List<Note> result = noteService.findAll(null, null, null, null, null, null, null, "owner-1");

        assertEquals(1, result.size());
        assertEquals("decrypted", result.get(0).getContent());
    }

    @Test
    void create_shouldInsertNoteAndHistory() {
        NoteInput input = new NoteInput();
        input.setTitle("New Note");
        input.setContent("New content");
        input.setTags(List.of("tag1", "tag2"));

        Note created = buildNote("generated-id", "owner-1");
        when(noteMapper.findById(anyString())).thenReturn(created);

        Note result = noteService.create(input, "owner-1");

        assertNotNull(result);
        verify(noteMapper).insert(any(Note.class));
        verify(historyMapper).insert(any());
        verify(tagSyncService).addTags(eq("owner-1"), anyList());
        verify(notificationService).notifyNoteCreated(anyString(), eq("owner-1"));
    }

    @Test
    void create_encryptedNote_shouldEncryptContent() {
        NoteInput input = new NoteInput();
        input.setTitle("Secret Note");
        input.setContent("secret content");
        input.setIsEncrypted(true);

        Note created = buildNote("id-1", "owner-1");
        when(noteMapper.findById(anyString())).thenReturn(created);
        when(encryptionService.encrypt("secret content")).thenReturn("encrypted-secret");

        noteService.create(input, "owner-1");

        verify(encryptionService).encrypt("secret content");
    }

    @Test
    void update_shouldUpdateNoteAndRecordHistory() {
        Note existing = buildNote("note-1", "owner-1");
        when(noteMapper.findById("note-1")).thenReturn(existing);

        NoteInput input = new NoteInput();
        input.setTitle("Updated Title");
        input.setContent("Updated content");

        Note result = noteService.update("note-1", input, "owner-1");

        assertNotNull(result);
        verify(noteMapper).update(any(Note.class));
        verify(historyMapper).insert(any());
    }

    @Test
    void delete_softDelete_shouldCallSoftDelete() {
        Note note = buildNote("note-1", "owner-1");
        when(noteMapper.findById("note-1")).thenReturn(note);

        noteService.delete("note-1", false, "owner-1");

        verify(noteMapper).softDelete(eq("note-1"), any(LocalDateTime.class));
        verify(noteMapper, never()).permanentDelete(any());
        verify(notificationService).notifyNoteDeleted("note-1", "owner-1");
    }

    @Test
    void delete_permanentDelete_shouldCallPermanentDelete() {
        Note note = buildNote("note-1", "owner-1");
        when(noteMapper.findById("note-1")).thenReturn(note);

        noteService.delete("note-1", true, "owner-1");

        verify(noteMapper).permanentDelete("note-1");
        verify(attachmentMapper).deleteByParent("note-1", "note");
        verify(tagSyncService).removeTagsIfUnused(eq("owner-1"), anyList());
    }

    @Test
    void delete_nonExistentNote_shouldThrowException() {
        when(noteMapper.findById("missing")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> noteService.delete("missing", false, "owner-1"));
    }

    @Test
    void archive_shouldCallMapperArchive() {
        Note note = buildNote("note-1", "owner-1");
        when(noteMapper.findById("note-1")).thenReturn(note);

        noteService.archive("note-1", "owner-1");

        verify(noteMapper).archive("note-1");
    }

    @Test
    void restore_shouldCallMapperRestore() {
        Note note = buildNote("note-1", "owner-1");
        when(noteMapper.findById("note-1")).thenReturn(note);

        noteService.restore("note-1", "owner-1");

        verify(noteMapper).restore("note-1");
    }

    @Test
    void getHistory_shouldReturnHistoryList() {
        Note note = buildNote("note-1", "owner-1");
        when(noteMapper.findById("note-1")).thenReturn(note);
        when(historyMapper.findByNoteId("note-1")).thenReturn(Collections.emptyList());

        var history = noteService.getHistory("note-1", "owner-1");

        assertNotNull(history);
        verify(historyMapper).findByNoteId("note-1");
    }

    @Test
    void shareWithUser_ownerCanShare() {
        Note note = buildNote("note-1", "owner-1");
        when(noteMapper.findById("note-1")).thenReturn(note);

        noteService.shareWithUser("note-1", "user-2", "owner-1");

        verify(noteMapper).shareWithUser(eq("note-1"), anyString());
    }

    @Test
    void shareWithUser_nonOwner_shouldThrowException() {
        Note note = buildNote("note-1", "owner-1");
        when(noteMapper.findById("note-1")).thenReturn(note);

        assertThrows(AccessDeniedException.class,
                () -> noteService.shareWithUser("note-1", "user-2", "not-owner"));
    }

    @Test
    void unshareWithUser_ownerCanUnshare() {
        Note note = buildNote("note-1", "owner-1");
        note.setSharedWith("[\"user-2\"]");
        when(noteMapper.findById("note-1")).thenReturn(note);

        noteService.unshareWithUser("note-1", "user-2", "owner-1");

        verify(noteMapper).shareWithUser(eq("note-1"), anyString());
    }

    @Test
    void unshareWithUser_nonOwner_shouldThrowException() {
        Note note = buildNote("note-1", "owner-1");
        when(noteMapper.findById("note-1")).thenReturn(note);

        assertThrows(AccessDeniedException.class,
                () -> noteService.unshareWithUser("note-1", "user-2", "not-owner"));
    }

    @Test
    void findSharedWithMe_shouldCallMapper() {
        when(noteMapper.findSharedWithMe("user-1")).thenReturn(Collections.emptyList());

        List<Note> result = noteService.findSharedWithMe("user-1");

        assertNotNull(result);
        verify(noteMapper).findSharedWithMe("user-1");
    }

    @Test
    void importNote_shouldCreateNoteWithDefaults() {
        Note created = buildNote("id-1", "owner-1");
        when(noteMapper.findById(anyString())).thenReturn(created);

        Note result = noteService.importNote("Title", "Content", "folder", null, "owner-1");

        assertNotNull(result);
        verify(noteMapper).insert(any(Note.class));
    }
}
