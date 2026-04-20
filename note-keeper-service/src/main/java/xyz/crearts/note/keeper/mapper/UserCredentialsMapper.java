package xyz.crearts.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.crearts.note.keeper.model.UserCredentials;

@Mapper
public interface UserCredentialsMapper {

    UserCredentials findByUserId(@Param("userId") String userId);

    void insert(UserCredentials credentials);

    void update(UserCredentials credentials);
}
