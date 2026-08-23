package xyz.crearts.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.crearts.note.keeper.model.UserSettings;

import java.util.List;

@Mapper
public interface UserSettingsMapper {

    UserSettings findById(@Param("id") String id);

    UserSettings findByTelegramWebhookSecret(@Param("secret") String secret);

    List<UserSettings> findDailyReportEnabled();

    void insert(UserSettings settings);

    void update(UserSettings settings);

    void upsert(UserSettings settings);

    void updateDailyReportLastSent(@Param("id") String id, @Param("date") String date);
}
