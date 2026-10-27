package dev.cogitor.note.keeper.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import dev.cogitor.note.keeper.dto.VaultFolderInput;
import dev.cogitor.note.keeper.dto.VaultItemInput;
import dev.cogitor.note.keeper.dto.VaultSyncResponse;
import dev.cogitor.note.keeper.model.VaultFolder;
import dev.cogitor.note.keeper.model.VaultItem;
import dev.cogitor.note.keeper.service.VaultService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vault")
@Tag(name = "Vault")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;

    @GetMapping("/folders")
    public List<VaultFolder> listFolders(@AuthenticationPrincipal String ownerId) {
        return vaultService.listFolders(ownerId);
    }

    @PostMapping("/folders")
    public ResponseEntity<VaultFolder> createFolder(@Valid @RequestBody VaultFolderInput input,
                                                    @AuthenticationPrincipal String ownerId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vaultService.createFolder(ownerId, input));
    }

    @PutMapping("/folders/{id}")
    public VaultFolder updateFolder(@PathVariable String id, @Valid @RequestBody VaultFolderInput input,
                                    @AuthenticationPrincipal String ownerId) {
        return vaultService.updateFolder(ownerId, id, input);
    }

    @DeleteMapping("/folders/{id}")
    public ResponseEntity<Void> deleteFolder(@PathVariable String id, @AuthenticationPrincipal String ownerId) {
        vaultService.deleteFolder(ownerId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/items")
    public List<VaultItem> listItems(
            @RequestParam(required = false) String folder,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String since,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @AuthenticationPrincipal String ownerId) {
        return vaultService.listItems(ownerId, folder, q, parseTime(since), includeDeleted);
    }

    @GetMapping("/items/{id}")
    public VaultItem getItem(@PathVariable String id, @AuthenticationPrincipal String ownerId) {
        return vaultService.getItem(ownerId, id);
    }

    @PostMapping("/items")
    public ResponseEntity<VaultItem> createItem(@Valid @RequestBody VaultItemInput input,
                                                @AuthenticationPrincipal String ownerId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vaultService.createItem(ownerId, input));
    }

    @PutMapping("/items/{id}")
    public VaultItem updateItem(@PathVariable String id,
                                @Valid @RequestBody VaultItemInput input,
                                @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                @AuthenticationPrincipal String ownerId) {
        return vaultService.updateItem(ownerId, id, input, parseRevision(ifMatch));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id, @AuthenticationPrincipal String ownerId) {
        vaultService.deleteItem(ownerId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sync")
    public VaultSyncResponse sync(@RequestParam(required = false) String since,
                                  @AuthenticationPrincipal String ownerId) {
        return vaultService.sync(ownerId, parseTime(since));
    }

    private static LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value);
    }

    private static Integer parseRevision(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            return null;
        }
        String cleaned = ifMatch.replace("\"", "").trim();
        return Integer.parseInt(cleaned);
    }
}
