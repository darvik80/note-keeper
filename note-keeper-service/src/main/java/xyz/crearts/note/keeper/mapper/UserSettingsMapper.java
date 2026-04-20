package xyz.crearts.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.crearts.note.keeper.model.UserSettings;

@Mapper
public interface UserSettingsMapper {

    UserSettings findById(@Param("id") String id);

    void insert(UserSettings settings);

    void update(UserSettings settings);

    void upsert(UserSettings settings);
}
