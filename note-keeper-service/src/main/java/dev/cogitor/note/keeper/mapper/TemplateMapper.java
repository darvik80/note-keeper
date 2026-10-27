package dev.cogitor.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import dev.cogitor.note.keeper.model.NoteTemplate;

import java.util.List;

@Mapper
public interface TemplateMapper {

    List<NoteTemplate> findAll(@Param("category") String category, @Param("ownerId") String ownerId);

    NoteTemplate findById(@Param("id") String id);

    void insert(NoteTemplate template);

    void delete(@Param("id") String id);
}
