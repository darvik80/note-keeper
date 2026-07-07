package xyz.crearts.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.crearts.note.keeper.model.SavedQuery;

import java.util.List;

@Mapper
public interface SavedQueryMapper {

    List<SavedQuery> findAllByOwner(@Param("ownerId") String ownerId);

    SavedQuery findByIdAndOwner(@Param("id") String id, @Param("ownerId") String ownerId);

    void insert(SavedQuery query);

    void delete(@Param("id") String id);
}
