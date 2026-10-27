package dev.cogitor.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import dev.cogitor.note.keeper.model.UserCredentials;

@Mapper
public interface UserCredentialsMapper {

    UserCredentials findByUserId(@Param("userId") String userId);

    void insert(UserCredentials credentials);

    void update(UserCredentials credentials);
}
