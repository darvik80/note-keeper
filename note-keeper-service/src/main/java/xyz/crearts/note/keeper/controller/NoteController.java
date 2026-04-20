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
    public Note getNoteById(@PathVariable String id) {
        return noteService.findById(id);
    }

    @PutMapping("/{id}")
    public Note updateNote(@PathVariable String id, @Valid @RequestBody NoteInput input) {
        return noteService.update(id, input);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean permanent) {
        noteService.delete(id, permanent);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/archive")
    public Note archiveNote(@PathVariable String id) {
        return noteService.archive(id);
    }

    @PostMapping("/{id}/restore")
    public Note restoreNote(@PathVariable String id) {
        return noteService.restore(id);
    }

    @GetMapping("/{id}/history")
    public List<NoteHistory> getNoteHistory(@PathVariable String id) {
        return noteService.getHistory(id);
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
}
