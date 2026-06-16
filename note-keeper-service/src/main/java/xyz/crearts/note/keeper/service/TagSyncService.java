package xyz.crearts.note.keeper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.mapper.NoteMapper;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.mapper.UserTagMapper;
import xyz.crearts.note.keeper.model.Note;
import xyz.crearts.note.keeper.model.Todo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Keeps the user_tag table in sync with tags on notes and todos.
 * Uses incremental updates: adds new tags immediately, removes only when
 * no other record of the same owner uses the tag.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagSyncService {

    private final UserTagMapper userTagMapper;
    private final NoteMapper noteMapper;
    private final TodoMapper todoMapper;

    /**
     * Add tags to user_tag table (idempotent via INSERT OR IGNORE).
     */
    public void addTags(String ownerId, List<String> tags) {
        if (tags == null) return;
        for (String tag : tags) {
            if (tag != null && !tag.isBlank()) {
                userTagMapper.insert(UUID.randomUUID().toString(), ownerId, tag);
            }
        }
    }

    /**
     * Remove tags from user_tag table only if no other note/todo of this owner uses them.
     */
    public void removeTagsIfUnused(String ownerId, List<String> tags) {
        if (tags == null) return;
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) continue;
            if (!isTagInUse(ownerId, tag)) {
                userTagMapper.deleteByName(ownerId, tag);
                log.debug("Removed unused tag '{}' for user {}", tag, ownerId);
            }
        }
    }

    /**
     * Handle tag diff on update: add new tags, remove old tags that are no longer used.
     */
    public void updateTags(String ownerId, List<String> oldTags, List<String> newTags) {
        Set<String> oldSet = normalize(oldTags);
        Set<String> newSet = normalize(newTags);

        // Tags to add
        List<String> toAdd = newSet.stream()
                .filter(t -> !oldSet.contains(t))
                .collect(Collectors.toList());
        addTags(ownerId, toAdd);

        // Tags to potentially remove
        List<String> toRemove = oldSet.stream()
                .filter(t -> !newSet.contains(t))
                .collect(Collectors.toList());
        removeTagsIfUnused(ownerId, toRemove);
    }

    /**
     * Rebuild user_tag table for a specific owner from scratch.
     * Useful for migration or repair.
     */
    public void rebuildForOwner(String ownerId) {
        // Clear existing
        List<String> existing = userTagMapper.findAllByOwner(ownerId);
        for (String tag : existing) {
            userTagMapper.deleteByName(ownerId, tag);
        }

        // Collect from all notes and todos
        Set<String> allTags = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        noteMapper.findAll(null, null, null, null, null, null, null, ownerId)
                .forEach(n -> { if (n.getTags() != null) allTags.addAll(n.getTags()); });
        todoMapper.findAll(null, null, null, null, null, null, ownerId)
                .forEach(t -> { if (t.getTags() != null) allTags.addAll(t.getTags()); });

        addTags(ownerId, new ArrayList<>(allTags));
        log.info("Rebuilt {} tags for user {}", allTags.size(), ownerId);
    }

    public List<String> getAllTags(String ownerId) {
        return userTagMapper.findAllByOwner(ownerId);
    }

    private boolean isTagInUse(String ownerId, String tag) {
        List<Note> notes = noteMapper.findAll(null, tag, null, null, null, null, null, ownerId);
        if (notes != null && !notes.isEmpty()) return true;
        List<Todo> todos = todoMapper.findAll(null, tag, null, null, null, null, ownerId);
        return todos != null && !todos.isEmpty();
    }

    private Set<String> normalize(List<String> tags) {
        if (tags == null) return Collections.emptySet();
        return tags.stream()
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toSet());
    }
}
