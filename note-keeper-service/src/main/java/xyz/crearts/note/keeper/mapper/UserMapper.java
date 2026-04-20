package xyz.crearts.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.crearts.note.keeper.model.User;
import xyz.crearts.note.keeper.model.UserCredentials;

import java.util.List;

@Mapper
public interface UserMapper {

    User findById(@Param("id") String id);

    User findByEmail(@Param("email") String email);

    User findByGoogleId(@Param("googleId") String googleId);

    void insert(User user);

    void update(User user);

    UserCredentials findCredentialsByUserId(@Param("userId") String userId);

    void insertCredentials(UserCredentials credentials);

    void updateCredentials(UserCredentials credentials);

    List<User> findByIds(@Param("ids") List<String> ids);
}
