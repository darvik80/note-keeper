package xyz.crearts.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.crearts.note.keeper.model.Attachment;

import java.util.List;

@Mapper
public interface AttachmentMapper {

    void insert(Attachment attachment);

    List<Attachment> findByParent(@Param("parentId") String parentId, @Param("parentType") String parentType);

    void deleteByParent(@Param("parentId") String parentId, @Param("parentType") String parentType);
}
