package xyz.crearts.note.keeper.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.crearts.note.keeper.dto.NoteInput;
import xyz.crearts.note.keeper.exception.ResourceNotFoundException;
import xyz.crearts.note.keeper.mapper.AttachmentMapper;
import xyz.crearts.note.keeper.mapper.NoteHistoryMapper;
import xyz.crearts.note.keeper.mapper.NoteMapper;
import xyz.crearts.note.keeper.model.Attachment;
import xyz.crearts.note.keeper.model.Note;
import xyz.crearts.note.keeper.model.NoteHistory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class NoteService {

    private final NoteMapper noteMapper;
    private final NoteHistoryMapper historyMapper;
    private final AttachmentMapper attachmentMapper;

    public NoteService(NoteMapper noteMapper, NoteHistoryMapper historyMapper, AttachmentMapper attachmentMapper) {
        this.noteMapper = noteMapper;
        this.historyMapper = historyMapper;
        this.attachmentMapper = attachmentMapper;
    }

    public List<Note> findAll(String folder, String tag, String priority,
                              Boolean isFavorite, Boolean isEncrypted,
                              Boolean isArchived, Boolean isDeleted, String ownerId) {
        List<Note> notes = noteMapper.findAll(folder, tag, priority, isFavorite, isEncrypted, isArchived, isDeleted, ownerId);
        for (Note note : notes) {
            note.setAttachments(attachmentMapper.findByParent(note.getId(), "note"));
            note.setHistory(historyMapper.findByNoteId(note.getId()));
        }
        return notes;
    }

    public Note findById(String id) {
        Note note = noteMapper.findById(id);
        if (note == null) {
            throw new ResourceNotFoundException("Note not found: " + id);
        }
        note.setAttachments(attachmentMapper.findByParent(note.getId(), "note"));
        note.setHistory(historyMapper.findByNoteId(note.getId()));
        return note;
    }

    @Transactional
    public Note create(NoteInput input, String ownerId) {
        Note note = new Note();
        note.setId(UUID.randomUUID().toString());
        note.setTitle(input.getTitle());
        note.setContent(input.getContent());
        note.setTags(input.getTags() != null ? input.getTags() : new ArrayList<>());
        note.setFolder(input.getFolder() != null ? input.getFolder() : "default");
        note.setSubfolder(input.getSubfolder());
        note.setPriority(input.getPriority() != null ? input.getPriority() : "medium");
        note.setFavorite(input.getIsFavorite() != null && input.getIsFavorite());
        note.setEncrypted(input.getIsEncrypted() != null && input.getIsEncrypted());
        note.setArchived(false);
        note.setDeleted(false);
        note.setOwnerId(ownerId);
        note.setSharedWith("[]");
        note.setReminder(input.getReminder());
        note.setTemplateId(input.getTemplateId());
        LocalDateTime now = LocalDateTime.now();
        note.setCreatedAt(now);
        note.setUpdatedAt(now);

        noteMapper.insert(note);

        NoteHistory history = new NoteHistory();
        history.setId(UUID.randomUUID().toString());
        history.setNoteId(note.getId());
        history.setContent(note.getContent());
        history.setTimestamp(now);
        history.setAction("created");
        historyMapper.insert(history);

        if (input.getAttachments() != null) {
            for (Attachment att : input.getAttachments()) {
                att.setId(UUID.randomUUID().toString());
                att.setParentId(note.getId());
                att.setParentType("note");
                if (att.getUploadedAt() == null) att.setUploadedAt(now);
                attachmentMapper.insert(att);
            }
        }

        return findById(note.getId());
    }

    @Transactional
    public Note update(String id, NoteInput input) {
        Note existing = findById(id);

        existing.setTitle(input.getTitle());
        existing.setContent(input.getContent());
        if (input.getTags() != null) existing.setTags(input.getTags());
        if (input.getFolder() != null) existing.setFolder(input.getFolder());
        existing.setSubfolder(input.getSubfolder());
        if (input.getPriority() != null) existing.setPriority(input.getPriority());
        if (input.getIsFavorite() != null) existing.setFavorite(input.getIsFavorite());
        if (input.getIsEncrypted() != null) existing.setEncrypted(input.getIsEncrypted());
        existing.setReminder(input.getReminder());
        existing.setTemplateId(input.getTemplateId());
        existing.setUpdatedAt(LocalDateTime.now());

        noteMapper.update(existing);

        NoteHistory history = new NoteHistory();
        history.setId(UUID.randomUUID().toString());
        history.setNoteId(id);
        history.setContent(existing.getContent());
        history.setTimestamp(existing.getUpdatedAt());
        history.setAction("edited");
        historyMapper.insert(history);

        if (input.getAttachments() != null) {
            attachmentMapper.deleteByParent(id, "note");
            for (Attachment att : input.getAttachments()) {
                att.setId(att.getId() != null ? att.getId() : UUID.randomUUID().toString());
                att.setParentId(id);
                att.setParentType("note");
                if (att.getUploadedAt() == null) att.setUploadedAt(LocalDateTime.now());
                attachmentMapper.insert(att);
            }
        }

        return findById(id);
    }

    @Transactional
    public void delete(String id, boolean permanent) {
        Note note = noteMapper.findById(id);
        if (note == null) {
            throw new ResourceNotFoundException("Note not found: " + id);
        }
        if (permanent) {
            attachmentMapper.deleteByParent(id, "note");
            noteMapper.permanentDelete(id);
        } else {
            noteMapper.softDelete(id, LocalDateTime.now().toString());
        }
    }

    public Note archive(String id) {
        findById(id);
        noteMapper.archive(id);
        return findById(id);
    }

    public Note restore(String id) {
        findById(id);
        noteMapper.restore(id);
        return findById(id);
    }

    public List<NoteHistory> getHistory(String id) {
        findById(id);
        return historyMapper.findByNoteId(id);
    }

    @Transactional
    public Note importNote(String title, String content, String folder, String subfolder, String ownerId) {
        NoteInput input = new NoteInput();
        input.setTitle(title);
        input.setContent(content);
        input.setFolder(folder != null ? folder : "default");
        input.setSubfolder(subfolder);
        input.setPriority("medium");
        return create(input, ownerId);
    }
}
