package xyz.crearts.note.keeper.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.dto.SavedQueryInput;
import xyz.crearts.note.keeper.dto.SearchResult;
import xyz.crearts.note.keeper.exception.ResourceNotFoundException;
import xyz.crearts.note.keeper.mapper.*;
import xyz.crearts.note.keeper.model.Note;
import xyz.crearts.note.keeper.model.SavedQuery;
import xyz.crearts.note.keeper.model.Todo;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final NoteMapper noteMapper;
    private final TodoMapper todoMapper;
    private final SavedQueryMapper savedQueryMapper;
    private final AttachmentMapper attachmentMapper;
    private final NoteHistoryMapper historyMapper;

    public SearchResult search(String query, String type, String tags, String priority, String ownerId) {
        List<Note> notes = Collections.emptyList();
        List<Todo> todos = Collections.emptyList();

        if (type == null || "all".equals(type) || "notes".equals(type)) {
            notes = noteMapper.search(query, tags, priority, ownerId);
            for (Note note : notes) {
                note.setAttachments(attachmentMapper.findByParent(note.getId(), "note"));
                note.setHistory(historyMapper.findByNoteId(note.getId()));
            }
        }
        if (type == null || "all".equals(type) || "todos".equals(type)) {
            todos = todoMapper.search(query, tags, priority, ownerId);
            for (Todo todo : todos) {
                todo.setAttachments(attachmentMapper.findByParent(todo.getId(), "todo"));
            }
        }

        return new SearchResult(notes, todos);
    }

    public List<SavedQuery> getSavedQueries(String ownerId) {
        return savedQueryMapper.findAllByOwner(ownerId);
    }

    public SavedQuery saveQuery(SavedQueryInput input, String ownerId) {
        SavedQuery query = new SavedQuery();
        query.setId(UUID.randomUUID().toString());
        query.setOwnerId(ownerId);
        query.setName(input.getName());
        query.setQuery(input.getQuery());
        query.setFilters(input.getFilters() != null ? input.getFilters() : new SavedQuery.Filters());
        query.setCreatedAt(LocalDateTime.now());

        savedQueryMapper.insert(query);
        return savedQueryMapper.findByIdAndOwner(query.getId(), ownerId);
    }

    public void deleteQuery(String id, String ownerId) {
        SavedQuery query = savedQueryMapper.findByIdAndOwner(id, ownerId);
        if (query == null) {
            throw new ResourceNotFoundException("Saved query not found: " + id);
        }
        savedQueryMapper.delete(id);
    }
}
