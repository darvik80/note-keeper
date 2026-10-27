package dev.cogitor.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import dev.cogitor.note.keeper.model.ApiToken;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ApiTokenMapper {

    ApiToken findById(@Param("id") String id);

    ApiToken findByHash(@Param("tokenHash") String tokenHash);

    List<ApiToken> findByUserId(@Param("userId") String userId);

    void insert(ApiToken token);

    void delete(@Param("id") String id, @Param("userId") String userId);

    void touchLastUsed(@Param("id") String id, @Param("lastUsedAt") LocalDateTime lastUsedAt);
}
