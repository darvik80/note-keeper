package xyz.crearts.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.crearts.note.keeper.model.Note;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NoteMapper {

    List<Note> findAll(@Param("folder") String folder,
                       @Param("tag") String tag,
                       @Param("priority") String priority,
                       @Param("isFavorite") Boolean isFavorite,
                       @Param("isEncrypted") Boolean isEncrypted,
                       @Param("isArchived") Boolean isArchived,
                       @Param("isDeleted") Boolean isDeleted,
                       @Param("ownerId") String ownerId);

    Note findById(@Param("id") String id);

    void insert(Note note);

    void update(Note note);

    void softDelete(@Param("id") String id, @Param("deletedAt") LocalDateTime deletedAt);

    void permanentDelete(@Param("id") String id);

    void archive(@Param("id") String id);

    void restore(@Param("id") String id);

    int countByDateRange(@Param("start") String start, @Param("end") String end, @Param("ownerId") String ownerId);

    List<Note> search(@Param("query") String query,
                      @Param("tags") String tags,
                      @Param("priority") String priority,
                      @Param("ownerId") String ownerId);

    int countByPriority(@Param("priority") String priority,
                        @Param("start") String start,
                        @Param("end") String end,
                        @Param("ownerId") String ownerId);

    List<Note> findSharedWithMe(@Param("userId") String userId);

    void shareWithUser(@Param("id") String id, @Param("sharedWith") String sharedWith);
}
