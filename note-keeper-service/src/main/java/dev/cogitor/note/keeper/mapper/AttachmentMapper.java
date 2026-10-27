package dev.cogitor.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import dev.cogitor.note.keeper.model.Attachment;

import java.util.List;

@Mapper
public interface AttachmentMapper {

    void insert(Attachment attachment);

    List<Attachment> findByParent(@Param("parentId") String parentId, @Param("parentType") String parentType);

    void deleteByParent(@Param("parentId") String parentId, @Param("parentType") String parentType);

    Attachment findById(@Param("id") String id);

    void deleteById(@Param("id") String id);
}
