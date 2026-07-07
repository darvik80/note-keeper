package xyz.crearts.note.keeper.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.crearts.note.keeper.dto.NoteInput;
import xyz.crearts.note.keeper.model.Note;
import xyz.crearts.note.keeper.model.NoteHistory;
import xyz.crearts.note.keeper.service.NoteService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Note controller with owner support.
 * Extracts owner ID from JWT token for all operations.
 */

@Slf4j
@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public List<Note> getNotes(
            @RequestParam(required = false) String folder,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Boolean isFavorite,
            @RequestParam(required = false) Boolean isEncrypted,
            @RequestParam(required = false) Boolean isArchived,
            @RequestParam(required = false) Boolean isDeleted,
            @AuthenticationPrincipal String ownerId) {
        log.info("GET /api/v1/notes - ownerId: {}", ownerId);
        return noteService.findAll(folder, tag, priority, isFavorite, isEncrypted, isArchived, isDeleted, ownerId);
    }

    @PostMapping
    public ResponseEntity<Note> createNote(@Valid @RequestBody NoteInput input,
                                           @AuthenticationPrincipal String ownerId) {
        log.info("POST /api/v1/notes - ownerId: {}, title: {}", ownerId, input.getTitle());
        Note note = noteService.create(input, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }

    @GetMapping("/{id}")
    public Note getNoteById(@PathVariable String id, @AuthenticationPrincipal String ownerId) {
        return noteService.findById(id, ownerId);
    }

    @PutMapping("/{id}")
    public Note updateNote(@PathVariable String id, @Valid @RequestBody NoteInput input,
                           @AuthenticationPrincipal String ownerId) {
        return noteService.update(id, input, ownerId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean permanent,
            @AuthenticationPrincipal String ownerId) {
        noteService.delete(id, permanent, ownerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/archive")
    public Note archiveNote(@PathVariable String id, @AuthenticationPrincipal String ownerId) {
        return noteService.archive(id, ownerId);
    }

    @PostMapping("/{id}/restore")
    public Note restoreNote(@PathVariable String id, @AuthenticationPrincipal String ownerId) {
        return noteService.restore(id, ownerId);
    }

    @GetMapping("/{id}/history")
    public List<NoteHistory> getNoteHistory(@PathVariable String id, @AuthenticationPrincipal String ownerId) {
        return noteService.getHistory(id, ownerId);
    }

    @PostMapping("/import")
    public ResponseEntity<Note> importNote(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String folder,
            @RequestParam(required = false) String subfolder,
            @AuthenticationPrincipal String ownerId) throws IOException {
        String filename = file.getOriginalFilename();
        String title = filename != null ? filename.replaceFirst("\\.[^.]+$", "") : "Imported Note";
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        Note note = noteService.importNote(title, content, folder, subfolder, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }

    @GetMapping("/shared-with-me")
    public List<Note> getSharedWithMe(@AuthenticationPrincipal String ownerId) {
        log.info("GET /api/v1/notes/shared-with-me - ownerId: {}", ownerId);
        return noteService.findSharedWithMe(ownerId);
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<Note> shareNote(
            @PathVariable String id,
            @RequestParam String userId,
            @AuthenticationPrincipal String ownerId) {
        log.info("POST /api/v1/notes/{}/share - ownerId: {}, userId: {}", id, ownerId, userId);
        Note note = noteService.shareWithUser(id, userId, ownerId);
        return ResponseEntity.ok(note);
    }

    @DeleteMapping("/{id}/share")
    public ResponseEntity<Note> unshareNote(
            @PathVariable String id,
            @RequestParam String userId,
            @AuthenticationPrincipal String ownerId) {
        log.info("DELETE /api/v1/notes/{}/share - ownerId: {}, userId: {}", id, ownerId, userId);
        Note note = noteService.unshareWithUser(id, userId, ownerId);
        return ResponseEntity.ok(note);
    }
}
