package xyz.crearts.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.crearts.note.keeper.model.NoteHistory;

import java.util.List;

@Mapper
public interface NoteHistoryMapper {

    void insert(NoteHistory history);

    List<NoteHistory> findByNoteId(@Param("noteId") String noteId);
}
