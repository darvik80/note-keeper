package xyz.crearts.note.keeper.service;

import lombok.extern.slf4j.Slf4j;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NoteService {

    private final NoteMapper noteMapper;
    private final NoteHistoryMapper historyMapper;
    private final AttachmentMapper attachmentMapper;
    private final EncryptionService encryptionService;
    private final NotificationService notificationService;
    private final TagSyncService tagSyncService;
    private final ResourceAccessService resourceAccess;

    public NoteService(NoteMapper noteMapper, NoteHistoryMapper historyMapper, 
                      AttachmentMapper attachmentMapper, EncryptionService encryptionService,
                      NotificationService notificationService, TagSyncService tagSyncService,
                      ResourceAccessService resourceAccess) {
        this.noteMapper = noteMapper;
        this.historyMapper = historyMapper;
        this.attachmentMapper = attachmentMapper;
        this.encryptionService = encryptionService;
        this.notificationService = notificationService;
        this.tagSyncService = tagSyncService;
        this.resourceAccess = resourceAccess;
    }

    public List<Note> findAll(String folder, String tag, String priority,
                              Boolean isFavorite, Boolean isEncrypted,
                              Boolean isArchived, Boolean isDeleted, String ownerId) {
        List<Note> notes = noteMapper.findAll(folder, tag, priority, isFavorite, isEncrypted, isArchived, isDeleted, ownerId);
        // Decrypt content for encrypted notes
        for (Note note : notes) {
            if (note.isEncrypted() && note.getContent() != null && !note.getContent().isEmpty()) {
                try {
                    note.setContent(encryptionService.decrypt(note.getContent()));
                } catch (Exception e) {
                    log.error("Failed to decrypt note {}: {}", note.getId(), e.getMessage());
                    note.setContent("[Decryption failed]");
                }
            }
        }
        return notes;
    }

    public Note findById(String id, String userId) {
        Note note = loadNote(id);
        resourceAccess.requireNoteRead(note, userId);
        return note;
    }

    private Note loadNote(String id) {
        Note note = noteMapper.findById(id);
        if (note == null) {
            throw new ResourceNotFoundException("Note not found: " + id);
        }
        decryptNoteContent(note);
        log.info("Loaded note {} with {} attachments", id, note.getAttachments() != null ? note.getAttachments().size() : 0);
        return note;
    }

    private void decryptNoteContent(Note note) {
        if (note.isEncrypted() && note.getContent() != null && !note.getContent().isEmpty()) {
            try {
                note.setContent(encryptionService.decrypt(note.getContent()));
            } catch (Exception e) {
                log.error("Failed to decrypt note {}: {}", note.getId(), e.getMessage());
                note.setContent("[Decryption failed - content may be corrupted]");
            }
        }
    }

    @Transactional
    public Note create(NoteInput input, String ownerId) {
        Note note = new Note();
        note.setId(UUID.randomUUID().toString());
        note.setTitle(input.getTitle());
        
        // Encrypt content if isEncrypted flag is set
        String content = input.getContent();
        boolean isEncrypted = input.getIsEncrypted() != null && input.getIsEncrypted();
        if (isEncrypted && content != null && !content.isEmpty()) {
            content = encryptionService.encrypt(content);
        }
        note.setContent(content);
        
        note.setTags(input.getTags() != null ? input.getTags() : new ArrayList<>());
        note.setFolder(input.getFolder() != null ? input.getFolder() : "default");
        note.setSubfolder(input.getSubfolder());
        note.setPriority(input.getPriority() != null ? input.getPriority() : "medium");
        note.setFavorite(input.getIsFavorite() != null && input.getIsFavorite());
        note.setEncrypted(isEncrypted);
        note.setArchived(false);
        note.setDeleted(false);
        note.setOwnerId(ownerId);
        note.setSharedWith("[]");
        note.setReminder(parseDate(input.getReminder()));
        note.setTemplateId(input.getTemplateId());
        LocalDateTime now = LocalDateTime.now();
        note.setCreatedAt(now);
        note.setUpdatedAt(now);

        noteMapper.insert(note);

        NoteHistory history = new NoteHistory();
        history.setId(UUID.randomUUID().toString());
        history.setNoteId(note.getId());
        history.setContent(content);
        history.setTimestamp(now);
        history.setAction("created");
        historyMapper.insert(history);

        if (input.getAttachments() != null && !input.getAttachments().isEmpty()) {
            log.info("Creating {} attachments for note {}", input.getAttachments().size(), note.getId());
            for (Attachment att : input.getAttachments()) {
                att.setId(UUID.randomUUID().toString());
                att.setParentId(note.getId());
                att.setParentType("note");
                if (att.getUploadedAt() == null) att.setUploadedAt(now);
                log.info("Saving attachment: {}", att.getName());
                attachmentMapper.insert(att);
            }
        } else {
            log.info("No attachments provided for note {}", note.getId());
        }

        tagSyncService.addTags(ownerId, note.getTags());
        notificationService.notifyNoteCreated(note.getId(), ownerId);
        return findById(note.getId(), ownerId);
    }

    @Transactional
    public Note update(String id, NoteInput input, String userId) {
        Note existing = loadNote(id);
        resourceAccess.requireNoteOwner(existing, userId);
        List<String> oldTags = existing.getTags() != null ? new ArrayList<>(existing.getTags()) : new ArrayList<>();

        existing.setTitle(input.getTitle());
        
        // Handle encryption for updated content
        String content = input.getContent();
        boolean wasEncrypted = existing.isEncrypted();
        boolean shouldBeEncrypted = input.getIsEncrypted() != null && input.getIsEncrypted();
        
        if (shouldBeEncrypted && content != null && !content.isEmpty()) {
            // If should be encrypted, encrypt the content
            // If it was already encrypted, we need to decrypt first then re-encrypt
            if (wasEncrypted) {
                // Content is already decrypted in findById, so just encrypt
                content = encryptionService.encrypt(content);
            } else {
                content = encryptionService.encrypt(content);
            }
        } else if (!shouldBeEncrypted && wasEncrypted) {
            // Was encrypted but now should not be - content is already decrypted
            // No action needed
        }
        
        existing.setContent(content);
        
        if (input.getTags() != null) existing.setTags(input.getTags());
        if (input.getFolder() != null) existing.setFolder(input.getFolder());
        existing.setSubfolder(input.getSubfolder());
        if (input.getPriority() != null) existing.setPriority(input.getPriority());
        if (input.getIsFavorite() != null) existing.setFavorite(input.getIsFavorite());
        if (input.getIsEncrypted() != null) existing.setEncrypted(shouldBeEncrypted);
        existing.setReminder(parseDate(input.getReminder()));
        existing.setTemplateId(input.getTemplateId());
        existing.setUpdatedAt(LocalDateTime.now());

        noteMapper.update(existing);

        NoteHistory history = new NoteHistory();
        history.setId(UUID.randomUUID().toString());
        history.setNoteId(id);
        history.setContent(content);
        history.setTimestamp(existing.getUpdatedAt());
        history.setAction("edited");
        historyMapper.insert(history);

        if (input.getAttachments() != null) {
            log.info("Updating attachments for note {}: {}", id, input.getAttachments().size());
            attachmentMapper.deleteByParent(id, "note");
            for (Attachment att : input.getAttachments()) {
                att.setId(att.getId() != null ? att.getId() : UUID.randomUUID().toString());
                att.setParentId(id);
                att.setParentType("note");
                if (att.getUploadedAt() == null) att.setUploadedAt(LocalDateTime.now());
                log.info("Saving attachment: {}", att.getName());
                attachmentMapper.insert(att);
            }
        }

        tagSyncService.updateTags(existing.getOwnerId(), oldTags, existing.getTags());
        notificationService.notifyNoteUpdated(id, existing.getOwnerId());
        return findById(id, userId);
    }

    @Transactional
    public void delete(String id, boolean permanent, String userId) {
        Note note = loadNote(id);
        resourceAccess.requireNoteOwner(note, userId);
        String ownerId = note.getOwnerId();
        List<String> tags = note.getTags();
        if (permanent) {
            attachmentMapper.deleteByParent(id, "note");
            noteMapper.permanentDelete(id);
            tagSyncService.removeTagsIfUnused(ownerId, tags);
        } else {
            noteMapper.softDelete(id, LocalDateTime.now());
        }
        notificationService.notifyNoteDeleted(id, ownerId);
    }

    public Note archive(String id, String userId) {
        Note note = loadNote(id);
        resourceAccess.requireNoteOwner(note, userId);
        noteMapper.archive(id);
        return findById(id, userId);
    }

    public Note restore(String id, String userId) {
        Note note = loadNote(id);
        resourceAccess.requireNoteOwner(note, userId);
        noteMapper.restore(id);
        return findById(id, userId);
    }

    public List<NoteHistory> getHistory(String id, String userId) {
        findById(id, userId);
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

    public List<Note> findSharedWithMe(String userId) {
        // MyBatis автоматически загружает attachments и history через noteWithCollectionsMap
        return noteMapper.findSharedWithMe(userId);
    }

    @Transactional
    public Note shareWithUser(String noteId, String userIdToAdd, String currentOwnerId) {
        Note note = loadNote(noteId);
        resourceAccess.requireNoteOwner(note, currentOwnerId);

        List<String> sharedUsers = resourceAccess.parseSharedWith(note.getSharedWith());
        if (!sharedUsers.contains(userIdToAdd)) {
            sharedUsers.add(userIdToAdd);
            String newSharedWith = toJsonArray(sharedUsers);
            note.setSharedWith(newSharedWith);
            noteMapper.shareWithUser(noteId, newSharedWith);
        }

        return findById(noteId, currentOwnerId);
    }

    @Transactional
    public Note unshareWithUser(String noteId, String userIdToRemove, String currentOwnerId) {
        Note note = loadNote(noteId);
        resourceAccess.requireNoteOwner(note, currentOwnerId);

        List<String> sharedUsers = resourceAccess.parseSharedWith(note.getSharedWith());
        sharedUsers.remove(userIdToRemove);
        String newSharedWith = toJsonArray(sharedUsers);
        note.setSharedWith(newSharedWith);
        noteMapper.shareWithUser(noteId, newSharedWith);

        return findById(noteId, currentOwnerId);
    }

    private String toJsonArray(List<String> list) {
        if (list.isEmpty()) {
            return "[]";
        }
        return "[" + list.stream()
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            // Parse ISO UTC format (with 'Z') from frontend — store as UTC LocalDateTime
            java.time.Instant instant = java.time.Instant.parse(dateStr);
            return LocalDateTime.ofInstant(instant, java.time.ZoneOffset.UTC);
        } catch (Exception e1) {
            try {
                // Try ISO local date-time format (treat as UTC for consistency)
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
                try {
                    // Try "yyyy-MM-dd" format
                    return java.time.LocalDate.parse(dateStr).atStartOfDay();
                } catch (Exception e3) {
                    // Ignore invalid date
                    return null;
                }
            }
        }
    }
}
