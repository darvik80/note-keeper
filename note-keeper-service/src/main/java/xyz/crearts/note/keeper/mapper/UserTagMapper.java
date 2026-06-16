package xyz.crearts.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserTagMapper {

    List<String> findAllByOwner(@Param("ownerId") String ownerId);

    void insert(@Param("id") String id, @Param("ownerId") String ownerId, @Param("tagName") String tagName);

    void deleteByName(@Param("ownerId") String ownerId, @Param("tagName") String tagName);

    int countByOwnerAndTag(@Param("ownerId") String ownerId, @Param("tagName") String tagName);
}
