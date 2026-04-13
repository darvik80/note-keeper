package xyz.crearts.note.keeper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.crearts.note.keeper.model.Note;
import xyz.crearts.note.keeper.model.Todo;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private List<Note> notes;
    private List<Todo> todos;
}
