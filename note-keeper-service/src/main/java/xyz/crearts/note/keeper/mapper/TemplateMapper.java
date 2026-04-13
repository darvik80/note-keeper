package xyz.crearts.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.crearts.note.keeper.model.NoteTemplate;

import java.util.List;

@Mapper
public interface TemplateMapper {

    List<NoteTemplate> findAll(@Param("category") String category);

    NoteTemplate findById(@Param("id") String id);

    void insert(NoteTemplate template);

    void delete(@Param("id") String id);
}
