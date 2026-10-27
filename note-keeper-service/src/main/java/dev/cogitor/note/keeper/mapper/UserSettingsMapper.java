package dev.cogitor.note.keeper.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import dev.cogitor.note.keeper.model.UserSettings;

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

    String findVaultKey(@Param("id") String id);

    void upsertVaultKey(@Param("id") String id, @Param("vaultKey") String vaultKey, @Param("updatedAt") java.time.LocalDateTime updatedAt);
}
