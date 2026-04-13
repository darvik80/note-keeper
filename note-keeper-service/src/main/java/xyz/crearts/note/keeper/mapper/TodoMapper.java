package xyz.crearts.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.crearts.note.keeper.model.Todo;

import java.util.List;

@Mapper
public interface TodoMapper {

    List<Todo> findAll(@Param("completed") Boolean completed,
                       @Param("tag") String tag,
                       @Param("priority") String priority,
                       @Param("isFavorite") Boolean isFavorite,
                       @Param("isArchived") Boolean isArchived,
                       @Param("isDeleted") Boolean isDeleted);

    Todo findById(@Param("id") String id);

    void insert(Todo todo);

    void update(Todo todo);

    void softDelete(@Param("id") String id, @Param("deletedAt") String deletedAt);

    void permanentDelete(@Param("id") String id);

    void archive(@Param("id") String id);

    void restore(@Param("id") String id);

    int countByDateRange(@Param("start") String start, @Param("end") String end);

    int countCompletedByDateRange(@Param("start") String start, @Param("end") String end);

    List<Todo> search(@Param("query") String query,
                      @Param("tags") String tags,
                      @Param("priority") String priority);

    int countByPriority(@Param("priority") String priority,
                        @Param("start") String start,
                        @Param("end") String end);
}
