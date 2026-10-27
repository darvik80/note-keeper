package dev.cogitor.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import dev.cogitor.note.keeper.model.VaultItem;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface VaultItemMapper {

    VaultItem findById(@Param("id") String id, @Param("ownerId") String ownerId);

    List<VaultItem> findByOwner(@Param("ownerId") String ownerId,
                                @Param("folderId") String folderId,
                                @Param("q") String q,
                                @Param("since") LocalDateTime since,
                                @Param("includeDeleted") boolean includeDeleted);

    List<VaultItem> findChangedSince(@Param("ownerId") String ownerId, @Param("since") LocalDateTime since);

    void insert(VaultItem item);

    void update(VaultItem item);

    void tombstone(@Param("id") String id, @Param("ownerId") String ownerId,
                   @Param("revision") int revision, @Param("deletedAt") LocalDateTime deletedAt);
}
