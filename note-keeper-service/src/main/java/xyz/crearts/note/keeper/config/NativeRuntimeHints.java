package xyz.crearts.note.keeper.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import xyz.crearts.note.keeper.client.DingTalkClient;
import xyz.crearts.note.keeper.client.TelegramClient;
import xyz.crearts.note.keeper.dto.*;
import xyz.crearts.note.keeper.mapper.*;
import xyz.crearts.note.keeper.mapper.typehandler.IntegerListTypeHandler;
import xyz.crearts.note.keeper.mapper.typehandler.LocalDateTimeTypeHandler;
import xyz.crearts.note.keeper.mapper.typehandler.StringListTypeHandler;
import xyz.crearts.note.keeper.model.*;

/**
 * GraalVM / Spring AOT hints: MyBatis XML, SQL init scripts, static UI, entity reflection.
 */
public class NativeRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("mapper/*.xml");
        hints.resources().registerPattern("schema.sql");
        hints.resources().registerPattern("schema-postgresql.sql");
        hints.resources().registerPattern("data.sql");
        hints.resources().registerPattern("data-postgresql.sql");
        hints.resources().registerPattern("static/*");
        hints.resources().registerPattern("static/**");
        hints.resources().registerPattern("application.yml");
        hints.resources().registerPattern("application-*.yml");

        MemberCategory[] members = MemberCategory.values();
        for (Class<?> type : new Class<?>[] {
                User.class, UserCredentials.class, UserSettings.class,
                Note.class, NoteHistory.class, NoteTemplate.class,
                Todo.class, Todo.Schedule.class, TodoCompletionLog.class,
                Attachment.class, SavedQuery.class,
                AuthRequest.class, AuthResponse.class, ErrorResponse.class,
                NoteInput.class, TodoInput.class, NoteTemplateInput.class, SavedQueryInput.class,
                SearchResult.class, IntegrationRequest.class, IntegrationResponse.class,
                AnalyticsResponse.class, AnalyticsResponse.TagCount.class,
                TelegramClient.TelegramResponse.class, TelegramClient.InlineButton.class,
                DingTalkClient.DingTalkResponse.class,
                LocalDateTimeTypeHandler.class, StringListTypeHandler.class, IntegerListTypeHandler.class
        }) {
            hints.reflection().registerType(type, members);
        }

        for (Class<?> mapper : new Class<?>[] {
                NoteMapper.class, TodoMapper.class, AttachmentMapper.class,
                UserMapper.class, UserCredentialsMapper.class, UserSettingsMapper.class,
                UserTagMapper.class, TemplateMapper.class, SavedQueryMapper.class,
                NoteHistoryMapper.class
        }) {
            hints.proxies().registerJdkProxy(mapper);
            hints.reflection().registerType(mapper, members);
        }
    }
}
