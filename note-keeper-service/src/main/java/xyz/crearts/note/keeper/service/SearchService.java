package xyz.crearts.note.keeper.service;

import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.dto.SavedQueryInput;
import xyz.crearts.note.keeper.dto.SearchResult;
import xyz.crearts.note.keeper.mapper.AttachmentMapper;
import xyz.crearts.note.keeper.mapper.NoteHistoryMapper;
import xyz.crearts.note.keeper.mapper.NoteMapper;
import xyz.crearts.note.keeper.mapper.SavedQueryMapper;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.model.Note;
import xyz.crearts.note.keeper.model.SavedQuery;
import xyz.crearts.note.keeper.model.Todo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class SearchService {

    private final NoteMapper noteMapper;
    private final TodoMapper todoMapper;
    private final SavedQueryMapper savedQueryMapper;
    private final AttachmentMapper attachmentMapper;
    private final NoteHistoryMapper historyMapper;

    public SearchService(NoteMapper noteMapper, TodoMapper todoMapper,
                         SavedQueryMapper savedQueryMapper, AttachmentMapper attachmentMapper,
                         NoteHistoryMapper historyMapper) {
        this.noteMapper = noteMapper;
        this.todoMapper = todoMapper;
        this.savedQueryMapper = savedQueryMapper;
        this.attachmentMapper = attachmentMapper;
        this.historyMapper = historyMapper;
    }

    public SearchResult search(String query, String type, String tags, String priority) {
        List<Note> notes = Collections.emptyList();
        List<Todo> todos = Collections.emptyList();

        if (type == null || "all".equals(type) || "notes".equals(type)) {
            notes = noteMapper.search(query, tags, priority);
            for (Note note : notes) {
                note.setAttachments(attachmentMapper.findByParent(note.getId(), "note"));
                note.setHistory(historyMapper.findByNoteId(note.getId()));
            }
        }
        if (type == null || "all".equals(type) || "todos".equals(type)) {
            todos = todoMapper.search(query, tags, priority);
            for (Todo todo : todos) {
                todo.setAttachments(attachmentMapper.findByParent(todo.getId(), "todo"));
            }
        }

        return new SearchResult(notes, todos);
    }

    public List<SavedQuery> getSavedQueries() {
        return savedQueryMapper.findAll();
    }

    public SavedQuery saveQuery(SavedQueryInput input) {
        SavedQuery query = new SavedQuery();
        query.setId(UUID.randomUUID().toString());
        query.setName(input.getName());
        query.setQuery(input.getQuery());
        query.setFilters(input.getFilters() != null ? input.getFilters() : new SavedQuery.Filters());
        query.setCreatedAt(LocalDateTime.now());

        savedQueryMapper.insert(query);

        List<SavedQuery> all = savedQueryMapper.findAll();
        return all.stream().filter(q -> q.getId().equals(query.getId())).findFirst().orElse(query);
    }

    public void deleteQuery(String id) {
        savedQueryMapper.delete(id);
    }
}
