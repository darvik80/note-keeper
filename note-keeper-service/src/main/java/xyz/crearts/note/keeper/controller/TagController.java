package xyz.crearts.note.keeper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.crearts.note.keeper.service.TagSyncService;

import java.util.List;

/**
 * Returns distinct tags from the user_tag table (O(1) lookup).
 */
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagSyncService tagSyncService;

    @GetMapping
    public List<String> getAllTags(@AuthenticationPrincipal String ownerId) {
        return tagSyncService.getAllTags(ownerId);
    }

    /**
     * Rebuild user_tag cache from existing notes/todos.
     * Useful after migration or data repair.
     */
    @PostMapping("/rebuild")
    public void rebuildTags(@AuthenticationPrincipal String ownerId) {
        tagSyncService.rebuildForOwner(ownerId);
    }
}
