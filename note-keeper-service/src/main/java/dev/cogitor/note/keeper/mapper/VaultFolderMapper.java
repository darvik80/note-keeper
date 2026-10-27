package dev.cogitor.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import dev.cogitor.note.keeper.model.VaultFolder;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface VaultFolderMapper {

    VaultFolder findById(@Param("id") String id, @Param("ownerId") String ownerId);

    List<VaultFolder> findByOwner(@Param("ownerId") String ownerId);

    List<VaultFolder> findChangedSince(@Param("ownerId") String ownerId, @Param("since") LocalDateTime since);

    void insert(VaultFolder folder);

    void update(VaultFolder folder);

    void delete(@Param("id") String id, @Param("ownerId") String ownerId);
}
